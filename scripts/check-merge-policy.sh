#!/usr/bin/env bash
# 合併後閘門：逐條機驗 scripts/merge-policy.txt，任何一項不符就 exit 非 0 並印出差異明細。
# 用途：跟 tgx-android/telegram-x 合併（或任何 rebase/cherry-pick）收尾時跑一次，
#       確認「已刪的沒被帶回來、gitlink 沒退回上游舊值、.gitmodules 的 branch 沒有幽靈分支、
#       藏在第二第三處的版號沒被改一半」。
# 用法：scripts/check-merge-policy.sh [--online] [政策檔路徑]
#   --online  另外用 git ls-remote 驗證 branch 那幾條在上游倉真的存在（會打網路，CI 預設不跑）
set -uo pipefail

ONLINE=0
POLICY=""
for arg in "$@"; do
  case "$arg" in
    --online) ONLINE=1 ;;
    -h|--help)
      sed -n '1,10p' "$0"
      exit 0
      ;;
    *) POLICY="$arg" ;;
  esac
done

ROOT="$(git rev-parse --show-toplevel 2>/dev/null || true)"
if [[ -z "$ROOT" ]]; then
  echo "[policy] FATAL: 不在 git 倉庫裡" >&2
  exit 2
fi
cd "$ROOT" || exit 2
[[ -n "$POLICY" ]] || POLICY="scripts/merge-policy.txt"
if [[ ! -f "$POLICY" ]]; then
  echo "[policy] FATAL: 找不到政策表 $POLICY" >&2
  exit 2
fi

fails=0
warns=0
checked=0

fail() {
  printf '%s\n' "  ✗ $*" >&2
  fails=$((fails + 1))
}
warn() {
  printf '%s\n' "  ⚠ $*" >&2
  warns=$((warns + 1))
}

tracked_all="$(git ls-files)"

# 先擋「還沒解完的衝突標記」：git config -f 讀不了帶標記的 .gitmodules，
# 那會讓下面報出幾十條「找不到段」的誤診，所以這裡先明確回報一次、branch 兩類檢查直接跳過。
GM_CONFLICT=0
if [[ -f .gitmodules ]] && grep -qE '^(<<<<<<<|>>>>>>>|={7})([[:space:]]|$)' .gitmodules; then
  GM_CONFLICT=1
  fail ".gitmodules 還帶著未解的合併衝突標記——先把 branch 那幾條解掉再重跑（本輪跳過 branch/nobranch 檢查，避免誤診）"
fi

# .gitmodules：兩趟解析，避免用 IFS 拆欄位時空欄位被吃掉
declare -A SEG_NAME=()    # path -> submodule 段名
declare -A PATH_OF=()     # 段名 -> path
declare -A SEG_BRANCH=()  # path -> branch 欄值（沒有 branch 欄的段不會進這張表）
while IFS= read -r line; do
  [[ -n "$line" ]] || continue
  key="${line%% *}"
  val="${line#* }"
  name="${key#submodule.}"
  name="${name%.path}"
  SEG_NAME["$val"]="$name"
  PATH_OF["$name"]="$val"
done < <(git config -f .gitmodules --get-regexp 'submodule\..*\.path$' 2>/dev/null || true)
while IFS= read -r line; do
  [[ -n "$line" ]] || continue
  key="${line%% *}"
  val="${line#* }"
  name="${key#submodule.}"
  name="${name%.branch}"
  p="${PATH_OF[$name]:-}"
  if [[ -n "$p" ]]; then
    SEG_BRANCH["$p"]="$val"
  fi
done < <(git config -f .gitmodules --get-regexp 'submodule\..*\.branch$' 2>/dev/null || true)

declare -A BRANCH_ROWS=()

while IFS='|' read -r kind path expected bad note || [[ -n "${kind:-}" ]]; do
  [[ -n "$kind" ]] || continue
  [[ "$kind" == "#"* ]] && continue
  checked=$((checked + 1))

  case "$kind" in
    deleted)
      n=$(printf '%s\n' "$tracked_all" | grep -Fxc -- "$path" || true)
      if [[ "$n" != "0" ]]; then
        fail "deleted: $path 又被追蹤了（$n 筆）—— 依政策應維持「不帶回來」"
      fi
      ;;
    deleted-any)
      hits=$(printf '%s\n' "$tracked_all" | grep -F -- "$path" || true)
      if [[ -n "$hits" ]]; then
        cnt=$(printf '%s\n' "$hits" | wc -l)
        fail "deleted-any: 有 $cnt 條追蹤路徑含「$path」（應為 0），前 10 條："
        printf '%s\n' "$hits" | head -10 | while IFS= read -r h; do printf '      %s\n' "$h" >&2; done
      fi
      ;;
    present)
      n=$(printf '%s\n' "$tracked_all" | grep -Fxc -- "$path" || true)
      if [[ "$n" == "0" ]]; then
        fail "present: $path 不見了（$note）"
      fi
      ;;
    gitlink)
      line=$(git ls-tree HEAD -- "$path")
      mode=$(printf '%s' "$line" | awk '{print $1}')
      sha=$(printf '%s' "$line" | awk '{print $3}')
      if [[ "$mode" != "160000" ]]; then
        fail "gitlink: $path 在 HEAD 不是子模組（mode='${mode:-空}'）"
      elif [[ "$sha" != "$expected" ]]; then
        if [[ -n "$bad" && "$bad" != "-" && "$sha" == "$bad" ]]; then
          fail "gitlink: $path 退回舊值 $sha（應為 $expected；$note）"
        else
          fail "gitlink: $path 是 $sha（應為 $expected；$note）"
        fi
      fi
      ;;
    branch)
      if [[ "$GM_CONFLICT" == "1" ]]; then
        continue
      fi
      BRANCH_ROWS["$path"]=1
      name="${SEG_NAME[$path]:-}"
      if [[ -z "$name" ]]; then
        fail "branch: .gitmodules 找不到 path=$path 的 submodule 段"
        continue
      fi
      cur="${SEG_BRANCH[$path]:-}"
      if [[ -z "$cur" ]]; then
        fail "branch: submodule.$name 缺少 branch 欄（應為 $expected；$note）"
      elif [[ "$cur" != "$expected" ]]; then
        fail "branch: submodule.$name.branch = $cur（應為 $expected；$note）"
      fi
      if [[ "$ONLINE" == "1" ]]; then
        url="$(git config -f .gitmodules --get "submodule.$name.url" 2>/dev/null || true)"
        if [[ -z "$url" ]]; then
          warn "branch(online): submodule.$name 沒有 url，無法驗證分支是否存在"
        elif out=$(git ls-remote --heads "$url" "$expected" 2>/dev/null); then
          if [[ -z "$out" ]]; then
            fail "branch(online): $url 沒有 refs/heads/$expected（$note）"
          fi
        else
          warn "branch(online): 查詢 $url 失敗（網路問題，不計失敗）"
        fi
      fi
      ;;
    nobranch)
      if [[ "$GM_CONFLICT" == "1" ]]; then
        continue
      fi
      BRANCH_ROWS["$path"]=1
      name="${SEG_NAME[$path]:-}"
      if [[ -z "$name" ]]; then
        fail "nobranch: .gitmodules 找不到 path=$path 的 submodule 段"
        continue
      fi
      if [[ -n "${SEG_BRANCH[$path]:-}" ]]; then
        fail "nobranch: submodule.$name 不該有 branch 欄（現在是 ${SEG_BRANCH[$path]}；$note）"
      fi
      ;;
    prop)
      if [[ ! -f "$path" ]]; then
        fail "prop: 找不到檔案 $path"
        continue
      fi
      if ! grep -Fq -- "$expected" "$path"; then
        fail "prop: $path 少了「$expected」（$note）"
      fi
      if [[ -n "$bad" && "$bad" != "-" ]]; then
        IFS=',' read -r -a bads <<<"$bad"
        for b in "${bads[@]}"; do
          [[ -n "$b" ]] || continue
          if grep -Fq -- "$b" "$path"; then
            fail "prop: $path 還有該被淘汰的「$b」（$note）"
          fi
        done
      fi
      ;;
    *)
      fail "政策表有未知的 kind：'$kind'（$path）"
      ;;
  esac
done < <(grep -v '^[[:space:]]*#' "$POLICY" | grep -v '^[[:space:]]*$')

# 反向封閉：.gitmodules 裡出現、但政策表沒登记的 branch 欄，一律視為未審
for p in "${!SEG_BRANCH[@]}"; do
  [[ -n "${SEG_BRANCH[$p]}" ]] || continue
  if [[ -z "${BRANCH_ROWS[$p]:-}" ]]; then
    fail "未登记的 branch 元數據：path=$p branch=${SEG_BRANCH[$p]}（政策表沒有對應條目，先補進 scripts/merge-policy.txt 再合併）"
  fi
done

# 提醒（不計失敗）：CI 的 runner 不做合併，rerere 是 per-clone 設定，本地沒開就提示
rr="$(git config --get rerere.enabled 2>/dev/null || true)"
if [[ "$rr" != "true" ]]; then
  warn "本 clone 沒開 rerere.enabled（目前是 '${rr:-未設定}'）；依慣例應由 scripts/setup.sh 或手動開啟"
fi

if ((fails > 0)); then
  printf '[policy] 失敗：%d 項不符（檢查 %d 條、警告 %d 條）\n' "$fails" "$checked" "$warns" >&2
  printf '[policy] 修法：照 scripts/merge-policy.txt 的 expected 值還原，或確認後連政策表一起更新再重跑\n' >&2
  exit 1
fi
printf '[policy] 通過：%d 條全部符合（警告 %d 條）\n' "$checked" "$warns"
exit 0

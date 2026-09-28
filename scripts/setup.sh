#!/bin/bash
set -e

# == Setup SDK & NDK ==
if [[ "$1" == "--skip-sdk-setup" ]]; then
  # shellcheck source=set-env.sh
  source "$(pwd)/scripts/set-env.sh"
else
  # shellcheck source=setup-sdk.sh
  source "$(pwd)/scripts/setup-sdk.sh"
fi

if [[ -f local.properties ]]; then
  echo -e "${STYLE_INFO}local.properties already exists.${STYLE_END}"
fi

# == Copy local.properties ===

if [[ ! -f local.properties ]]; then
  setup-properties.sh
fi

# == 讓「跟 tgx 合併反覆撞同一個衝突」變成自動解 ==
# rerere 是 per-clone 設定、不會跟著 commit 走，所以每個 clone（含 CI 與新機器）都要開一次。
# 方向由 scripts/merge-policy.txt 決定，收尾由 scripts/check-merge-policy.sh 把關（CI 兩條 workflow 都跑）。
git config rerere.enabled true
git config rerere.autoupdate true
echo -e "${STYLE_INFO}rerere enabled (衝突解法會自動記錄與復用)${STYLE_END}"

echo -e "${STYLE_INFO}Configure finished!${STYLE_END}"


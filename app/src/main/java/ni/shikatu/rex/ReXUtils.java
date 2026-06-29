package ni.shikatu.rex;

import org.drinkless.tdlib.TdApi;

public class ReXUtils {
    public static TdApi.MessageProperties emptyReplyMessageProperties(){
        return new TdApi.MessageProperties(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            true,
            true,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false
        );
    }
}

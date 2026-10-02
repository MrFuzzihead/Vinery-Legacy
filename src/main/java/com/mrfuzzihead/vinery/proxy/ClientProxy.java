package com.mrfuzzihead.vinery.proxy;

/**
 * Client-side lifecycle hook.
 *
 * <p>
 * Referenced by name from {@code @SidedProxy}, so the class name and package must stay in sync
 * with {@link CommonProxy}.
 */
public class ClientProxy extends CommonProxy {

    @Override
    public boolean isClient() {
        return true;
    }
}

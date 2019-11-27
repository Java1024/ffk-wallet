package com.ffk.fabric.chaincode;

public class Wallet {
    private String walletId;
    private Double tokenAmount;

    public Wallet(String walletId, Double tokenAmount) {
        this.walletId = walletId;
        this.tokenAmount = tokenAmount;
    }

    private Wallet() {}

    public String getWalletId() {
        return walletId;
    }

    public Double getTokenAmount() {
        return tokenAmount;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public void setTokenAmount(Double tokenAmount) {
        this.tokenAmount = tokenAmount;
    }

}

package com.ffk.fabric.chaincode;

import java.util.List;

import javax.xml.ws.Response;

import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ResponseUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AccountBasedChaincode extends ChaincodeBase {

    private class ChaincodeResponse {
        public String message;
        public String code;
        public boolean OK;

        public ChaincodeResponse(String message, String code, boolean OK) {
            this.code = code;
            this.message = message;
            this.OK = OK;
        }
    }

    private String responseError(String errorMessage, String code) {
        try {
            return (new ObjectMapper()).writeValueAsString(new ChaincodeResponse(errorMessage, code, false));
        } catch (Throwable e) {
            return "{\"code\":'" + code + "', \"message\":'" + e.getMessage() + " AND " + errorMessage + "', \"OK\":"
                + false + "}";
        }
    }

    private String responseSuccess(String successMessage) {
        try {
            return (new ObjectMapper()).writeValueAsString(new ChaincodeResponse(successMessage, "", true));
        } catch (Throwable e) {
            return "{\"message\":'" + e.getMessage() + " BUT " + successMessage + " (NO COMMIT)', \"OK\":" + false
                + "}";
        }
    }

    private String responseSuccessObject(String object) {
        return "{\"message\":" + object + ", \"OK\":" + true + "}";
    }

    private boolean checkString(String str) {
        if (str.trim().length() <= 0 || str == null)
            return false;
        return true;
    }

    @Override
    public Response init(ChaincodeStub stub) {
        return ResponseUtils.newSuccessResponse(responseSuccess("Init"));
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        String func = stub.getFunction();
        List<String> params = stub.getParameters();
        if (func.equals("createWallet"))
            return createWallet(stub, params);
        else if (func.equals("getWallet"))
            return getWallet(stub, params);
        else if (func.equals("transfer"))
            return transfer(stub, params);
        return ResponseUtils.newErrorResponse(responseError("Unsupported method", ""));
    }

    private Response createWallet(ChaincodeStub stub, List<String> args) {
        if (args.size() != 2)
            return ResponseUtils.newErrorResponse(responseError("Incorrect number of arguments, expecting 2", ""));
        String walletId = args.get(0);
        String tokenAmount = args.get(1);
        if (!checkString(walletId) || !checkString(tokenAmount))
            return ResponseUtils.newErrorResponse(responseError("Invalid argument(s)", ""));

        double tokenAmountDouble = 0.0;
        try {
            tokenAmountDouble = Double.parseDouble(tokenAmount);
            if (tokenAmountDouble < 0.0)
                return ResponseUtils.newErrorResponse(responseError("Invalid token amount", ""));
        } catch (NumberFormatException e) {
            return ResponseUtils.newErrorResponse(responseError("parseInt error", ""));
        }

        Wallet wallet = new Wallet(walletId, tokenAmountDouble);
        try {
            if (checkString(stub.getStringState(walletId)))
                return ResponseUtils.newErrorResponse(responseError("Existent wallet", ""));
            stub.putState(walletId, (new ObjectMapper()).writeValueAsBytes(wallet));
            return ResponseUtils.newSuccessResponse(responseSuccess("Wallet created"));
        } catch (Throwable e) {
            return ResponseUtils.newErrorResponse(responseError(e.getMessage(), ""));
        }
    }

    private Response getWallet(ChaincodeStub stub, List<String> args) {
        if (args.size() != 1)
            return ResponseUtils.newErrorResponse(responseError("Incorrect number of arguments, expecting 1", ""));
        String walletId = args.get(0);
        if (!checkString(walletId))
            return ResponseUtils.newErrorResponse(responseError("Invalid argument", ""));
        try {
            String walletString = stub.getStringState(walletId);
            if (!checkString(walletString))
                return ResponseUtils.newErrorResponse(responseError("Nonexistent wallet", ""));
            return ResponseUtils
                .newSuccessResponse((new ObjectMapper()).writeValueAsBytes(responseSuccessObject(walletString)));
        } catch (Throwable e) {
            return ResponseUtils.newErrorResponse(responseError(e.getMessage(), ""));
        }
    }

    private Response transfer(ChaincodeStub stub, List<String> args) {
        if (args.size() != 3)
            return ResponseUtils.newErrorResponse(responseError("Incorrect number of arguments, expecting 3", ""));
        String fromWalletId = args.get(0);
        String toWalletId = args.get(1);
        String tokenAmount = args.get(2);
        if (!checkString(fromWalletId) || !checkString(toWalletId) || !checkString(tokenAmount))
            return ResponseUtils.newErrorResponse(responseError("Invalid argument(s)", ""));
        if (fromWalletId.equals(toWalletId))
            return ResponseUtils.newErrorResponse(responseError("From-wallet is same as to-wallet", ""));

        double tokenAmountDouble = 0.0;
        try {
            tokenAmountDouble = Double.parseDouble(tokenAmount);
            if (tokenAmountDouble < 0.0)
                return ResponseUtils.newErrorResponse(responseError("Invalid token amount", ""));
        } catch (NumberFormatException e) {
            return ResponseUtils.newErrorResponse(responseError("parseDouble error", ""));
        }

        try {
            String fromWalletString = stub.getStringState(fromWalletId);
            if (!checkString(fromWalletString))
                return ResponseUtils.newErrorResponse(responseError("Nonexistent from-wallet", ""));
            String toWalletString = stub.getStringState(toWalletId);
            if (!checkString(toWalletString))
                return ResponseUtils.newErrorResponse(responseError("Nonexistent to-wallet", ""));

            ObjectMapper objectMapper = new ObjectMapper();
            Wallet fromWallet = objectMapper.readValue(fromWalletString, Wallet.class);
            Wallet toWallet = objectMapper.readValue(toWalletString, Wallet.class);

            if (fromWallet.getTokenAmount() < tokenAmountDouble)
                return ResponseUtils.newErrorResponse(responseError("Token amount not enough", ""));

            fromWallet.setTokenAmount(fromWallet.getTokenAmount() - tokenAmountDouble);
            toWallet.setTokenAmount(toWallet.getTokenAmount() + tokenAmountDouble);
            stub.putState(fromWalletId, objectMapper.writeValueAsBytes(fromWallet));
            stub.putState(toWalletId, objectMapper.writeValueAsBytes(toWallet));

            return ResponseUtils.newSuccessResponse(responseSuccess("Transferred"));
        } catch (Throwable e) {
            return ResponseUtils.newErrorResponse(responseError(e.getMessage(), ""));
        }
    }

    public static void main(String[] args) {
        new AccountBasedChaincode().start(args);
    }

}

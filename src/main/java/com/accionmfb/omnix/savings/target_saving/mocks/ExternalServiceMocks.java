package com.accionmfb.omnix.savings.target_saving.mocks;

import com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes;
import com.accionmfb.omnix.savings.target_saving.payload.response.AccountBalanceResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.FundsTransferResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.SMSResponsePayload;
import com.accionmfb.omnix.savings.target_saving.payload.response.TargetSavingsResponsePayload;

import static com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes.INSUFFICIENT_BALANCE;
import static com.accionmfb.omnix.savings.target_saving.constant.ResponseCodes.SERVICE_UNAVAILABLE;

public class ExternalServiceMocks
{
    /**
     * Mocks the funds transfer service with a given status. the status can be 'success', 'failure'
     * or 'unavailable'.
     * @param status
     * @return
     */
    public static FundsTransferResponsePayload mockFundsTransfer(String status)
    {
        FundsTransferResponsePayload responsePayload = new FundsTransferResponsePayload();
        if (status.equalsIgnoreCase("success")){
            responsePayload.setResponseCode("00");
            responsePayload.setMobileNumber("07035413941");
            responsePayload.setStatus("SUCCESS");
            responsePayload.setCreditAccountName("Accion Bank");
            responsePayload.setCreditAccount("8000200099");
            responsePayload.setDebitAccountName("Sunday Enyinna Lucky");
            responsePayload.setDebitAccount("2208497877");
            responsePayload.setTransRef("TRANS-12345");
            responsePayload.setAmount("1000");
            responsePayload.setNarration("Funds Transfer for savings");
            responsePayload.setT24TransRef("T24-4546-ref");
            responsePayload.setHandshakeId("Handshake-12345");
            return responsePayload;
        }else if(status.equalsIgnoreCase("unavailable")){
            responsePayload.setResponseCode(SERVICE_UNAVAILABLE.getResponseCode());
            responsePayload.setResponseMessage("Service is unavailable");
            return responsePayload;
        } else if (status.equalsIgnoreCase("insufficient funds")) {
            responsePayload.setResponseCode(INSUFFICIENT_BALANCE.getResponseCode());
            responsePayload.setResponseMessage("Insufficient balance");
            return responsePayload;
        } else
        {
            responsePayload.setResponseCode("07");
            responsePayload.setResponseMessage("Failed transaction");
            return responsePayload;
        }
    }

    public static SMSResponsePayload mockSMSResponse(String status)
    {
        SMSResponsePayload payload = new SMSResponsePayload();
        if(status.equalsIgnoreCase("success")){
            payload.setResponseCode("00");
            payload.setMobileNumber("07035413941");
            payload.setSmsFor("Sunday");
            return payload;
        }
        return payload;
    }

    public static AccountBalanceResponsePayload mockAccountBalance(String status){
        AccountBalanceResponsePayload responsePayload = new AccountBalanceResponsePayload();
        if(status.equalsIgnoreCase("success")){
            responsePayload.setResponseCode("00");
            responsePayload.setAccountNumber("2208497877");
            responsePayload.setAvailableBalance("5000");
            responsePayload.setLedgerBalance("5000");
            return responsePayload;
        }
        else if(status.equalsIgnoreCase("unavailable")){
            responsePayload.setResponseCode(SERVICE_UNAVAILABLE.getResponseCode());
            responsePayload.setResponseMessage("Account balance: Service unavailable");
            return responsePayload;
        }
        else{
            responsePayload.setResponseCode("99");
            responsePayload.setResponseMessage("Account balance transaction failed");
            return responsePayload;
        }
    }

    public static String mockAccountStatus(String status)
    {
        if(status.equalsIgnoreCase("success"))
            return "ACTIVE";
        else if (status.equalsIgnoreCase("unknown"))
            return "UNKNOWN";
        else
            return "UNAVAILABLE";
    }

    public static TargetSavingsResponsePayload mockTermination(){
        TargetSavingsResponsePayload responsePayload = new TargetSavingsResponsePayload();
        responsePayload.setResponseCode("00");
        return responsePayload;
    }
}

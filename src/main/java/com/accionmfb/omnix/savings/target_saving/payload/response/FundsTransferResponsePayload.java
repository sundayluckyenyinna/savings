package com.accionmfb.omnix.savings.target_saving.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundsTransferResponsePayload
{
    private String responseMessage;
    private String debitAccount;
    private String debitAccountName;
    private String creditAccount;
    private String creditAccountName;
    private String amount;
    private String narration;
    private String responseCode;
    private String transRef;
    private String status;
    private String handshakeId;
    private String t24TransRef;
    private String mobileNumber;

}

package net.corda.examples.bikemarket.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.types.TokenPointer;
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemNonFungibleTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemNonFungibleTokensHandler;
import net.corda.examples.bikemarket.states.FrameTokenState;
import net.corda.examples.bikemarket.states.WheelsTokenState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

public class TotalParts {


    private TotalParts(){
        //Instantiation not allowed
    }

    @InitiatingFlow
    @StartableByRPC
    public static class TotalPart extends FlowLogic<String> {

        private String frameModel;
        private String wheelModel;
        private boolean frame = false;


        public TotalPart(String part, String serialNumber) {
            if (part.equals("frame")) {
                this.frame = true;
                this.frameModel = serialNumber;
            } else {
                this.wheelModel = serialNumber;
            }
        }

        @Suspendable
        @Override
        public String call() throws FlowException {

            if (frame) {
                StateAndRef<FrameTokenState> frameStateAndRef = getServiceHub().getVaultService().
                        queryBy(FrameTokenState.class).getStates().stream()
                        .filter(sf -> sf.getState().getData().getModelNum().equals(this.frameModel)).findAny()
                        .orElseThrow(() -> new IllegalArgumentException("StockState symbol=\"" + this.frameModel + "\" not found from vault"));

                //get the TokenType object
                FrameTokenState frametokentype = frameStateAndRef.getState().getData();
                Party issuer = frametokentype.getIssuer();

                //get the pointer to the frame
                TokenPointer frametokenPointer = frametokentype.toPointer(frametokentype.getClass());

                SignedTransaction stx = subFlow(new RedeemNonFungibleTokens(frametokenPointer, issuer));
                return "\nThe frame part is totaled, and the token is redeem to BikeCo" + "\nTransaction ID: " + stx.getId();

            } else {
                //Step 2: Wheels Token
                StateAndRef<WheelsTokenState> wheelStateStateAndRef = getServiceHub().getVaultService().
                        queryBy(WheelsTokenState.class).getStates().stream().filter(sf -> sf.getState().getData().getModelNum().equals(this.wheelModel)).findAny()
                        .orElseThrow(() -> new IllegalArgumentException("StockState symbol=\""
                                + this.wheelModel + "\" not found from vault"));

                //get the TokenType object
                WheelsTokenState wheeltokentype = wheelStateStateAndRef.getState().getData();
                Party issuer = wheeltokentype.getIssuer();

                //get the pointer pointer to the wheel
                TokenPointer wheeltokenPointer = wheeltokentype.toPointer(wheeltokentype.getClass());

                SignedTransaction stx = subFlow(new RedeemNonFungibleTokens(wheeltokenPointer, issuer));
                return "\nThe wheels part is totaled, and the token is redeem to BikeCo" + "\nTransaction ID: " + stx.getId();
            }
        }
    }

    @InitiatedBy(TotalPart.class)
    public static class TotalPartResponder extends FlowLogic<Void>{

        private FlowSession counterSession;

        public TotalPartResponder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            // Simply use the MoveFungibleTokensHandler as the responding flow
            subFlow(new RedeemNonFungibleTokensHandler(counterSession));
            return null;
        }
    }
}
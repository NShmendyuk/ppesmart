package ru.itmo.ppesmart;

import com.owlike.genson.Genson;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "ppesmart",
        info = @Info(
                title = "PPE Transfer",
                description = "The hyperlegendary ppe transfer for Gazprom's PPE",
                version = "1.0.0-SNAPSHOT",
                contact = @Contact(
                        email = "NShmendyuk.ru@gmail.com")))
@Default
@NoArgsConstructor
public final class PPETransfer implements ContractInterface {

    private final Genson genson = new Genson();

    private enum PPETransferErrors {
        PPE_NOT_FOUND,
        PPE_ALREADY_EXISTS
    }

    /**
     * Creates some initial ppes on the ledger.
     *
     * @param ctx the transaction context
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void initTestLedger(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
    }

    /**
     * Creates a new ppe on the ledger.
     *
     * @param ctx the transaction context
     * @param ownerName the employee name
     * @param ownerID employee's personnel number
     * @param name PPE name
     * @param status PPE status
     * @param price PPE price
     * @param inventoryNumber PPE inventory number in subsidiary
     * @param startUseDate a date when PPE using has been started
     * @param lifeTime a life time to use PPE
     * @param subsidiary a company owned PPE / company where the employee is located
     * @return the created ppe
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public PPE createPPE(final Context ctx, final String ownerName,
                           final String ownerID, final String name, final String status,
                           final Float price, final String inventoryNumber,
                           final String startUseDate, final Integer lifeTime,
                           final String subsidiary) {
        ChaincodeStub stub = ctx.getStub();

        if (isPPEExist(ctx, inventoryNumber)) {
            String errorMessage = String.format("PPE %s already exists", inventoryNumber);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, PPETransferErrors.PPE_ALREADY_EXISTS.toString());
        }

        PPE ppe = new PPE(ownerName, ownerID, name, status, price, inventoryNumber, startUseDate, lifeTime, subsidiary);
        System.out.println("ADD PPE: " + ppe);
        String ppeJSON = genson.serialize(ppe);
        stub.putStringState(inventoryNumber, ppeJSON);

        return ppe;
    }

    /**
     * Retrieves an ppe with the specified ID from the ledger.
     *
     * @param ctx the transaction context
     * @param inventoryNumber current inventory number of ppe
     * @return the ppe found on the ledger if there was one
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public PPE readPPE(final Context ctx, final String inventoryNumber) {
        ChaincodeStub stub = ctx.getStub();
        String ppeJSON = stub.getStringState(inventoryNumber);

        if (ppeJSON == null || ppeJSON.isEmpty()) {
            String errorMessage = String.format("ppe with inventory number %s does not exist", inventoryNumber);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, PPETransferErrors.PPE_NOT_FOUND.toString());
        }
        PPE ppe = genson.deserialize(ppeJSON, PPE.class);
        return ppe;
    }

    /**
     * Retrieves an ppe history with the specified ID from the ledger.
     *
     * @param ctx the transaction context
     * @param inventoryNumber current inventory number of ppe
     * @return the ppe found on the ledger
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String readPPEHistory(final Context ctx, final String inventoryNumber) {
        ChaincodeStub stub = ctx.getStub();
        QueryResultsIterator<KeyModification> ppeHistory = stub.getHistoryForKey(inventoryNumber);

        if (ppeHistory == null) {
            String errorMessage = String.format("PPE %s history does not exist", inventoryNumber);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, PPETransferErrors.PPE_NOT_FOUND.toString());
        }
        StringBuilder infoHistoryString = new StringBuilder();
        infoHistoryString.append("[");
        while (ppeHistory.iterator().hasNext()) {
            KeyModification keyModification = ppeHistory.iterator().next();
            infoHistoryString.append(keyModification.getStringValue()).append(", ");
        }
        infoHistoryString.deleteCharAt(infoHistoryString.length() - 1).deleteCharAt(infoHistoryString.length() - 1).append("]");

        return infoHistoryString.toString();
    }

    /**
     * Updates the properties of an ppe on the ledger.
     *
     * @param ctx the transaction context=
     * @param ownerID employee's personnel number being updated
     * @param price PPE price being updated
     * @param inventoryNumber PPE inventory number in subsidiary being updated
     * @param subsidiary a company owned PPE / company where the employee is located being updated=
     * @return the transferred ppe
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public PPE updatePPE(final Context ctx, final String ownerName,
                           final String ownerID, final String name, final String status,
                           final Float price, final String inventoryNumber,
                           final String startUseDate, final Integer lifeTime,
                           final String subsidiary) {
        ChaincodeStub stub = ctx.getStub();

        if (!isPPEExist(ctx, inventoryNumber)) {
            String errorMessage = String.format("ppe %s does not exist", inventoryNumber);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, PPETransferErrors.PPE_NOT_FOUND.toString());
        }

        PPE newPPE = new PPE(ownerName, ownerID, name, status, price, inventoryNumber, startUseDate, lifeTime, subsidiary);
        String newPPEJSON = genson.serialize(newPPE);
        stub.putStringState(inventoryNumber, newPPEJSON);
        System.out.println("Update with: " + newPPE.toString());

        return newPPE;
    }

    /**
     * Deletes ppe on the ledger.
     *
     * @param ctx the transaction context
     * @param inventoryNumber PPE inventory number
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deletePPE(final Context ctx, final String inventoryNumber) {
        ChaincodeStub stub = ctx.getStub();

        if (!isPPEExist(ctx, inventoryNumber)) {
            String errorMessage = String.format("ppe %s does not exist", inventoryNumber);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, PPETransferErrors.PPE_NOT_FOUND.toString());
        }

        System.out.println("ppe {" + stub.getStringArgs().toString() + "} deleted");
        stub.delState(inventoryNumber);
    }

    /**
     * Checks the existence of the ppe on the ledger
     *
     * @param ctx the transaction context
     * @param inventoryNumber PPE inventory number
     * @return boolean indicating the existence of the ppe
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean isPPEExist(final Context ctx, final String inventoryNumber) {
        ChaincodeStub stub = ctx.getStub();
        String ppeJSON = stub.getStringState(inventoryNumber);

        return (ppeJSON != null && !ppeJSON.isEmpty());
    }

    /**
     * Changes the owner of a ppe on the ledger.
     *
     * @param ctx the transaction context
     * @param transferToSubsidiary the company to which the employee is transferred
     * @param transferStatus marker of transfering status
     * @return the updated for transfer ppe
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public PPE transferPPE(final Context ctx, final String inventoryNumber, final String transferToSubsidiary, final String transferStatus) {
        ChaincodeStub stub = ctx.getStub();
        String ppeJSON = stub.getStringState(inventoryNumber);

        if (ppeJSON == null || ppeJSON.isEmpty()) {
            String errorMessage = String.format("ppe %s does not exist", inventoryNumber);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, PPETransferErrors.PPE_NOT_FOUND.toString());
        }

        PPE ppe = genson.deserialize(ppeJSON, PPE.class);

        PPE newPPE = new PPE(ppe.getOwnerName(), ppe.getOwnerID(), ppe.getName(), transferStatus, ppe.getPrice(),
                ppe.getInventoryNumber(), ppe.getStartUseDate(), ppe.getLifeTime(), transferToSubsidiary);
        String newPPEJSON = genson.serialize(newPPE);
        stub.putStringState(inventoryNumber, newPPEJSON);

        return newPPE;
    }

    /**
     *
     * @param ctx the transaction context
     * @param inventoryNumber ppe inventory number for apply tranfering process
     * @param status marker to change status of ppe as applied
     * @return applied PPE
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public PPE applyTransferPPE(final Context ctx, final String inventoryNumber, final String status) {
        ChaincodeStub stub = ctx.getStub();
        String ppeJSON = stub.getStringState(inventoryNumber);

        if (ppeJSON == null || ppeJSON.isEmpty()) {
            String errorMessage = String.format("ppe %s does not exist", inventoryNumber);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, PPETransferErrors.PPE_NOT_FOUND.toString());
        }

        PPE ppe = genson.deserialize(ppeJSON, PPE.class);

        PPE newPPE = new PPE(ppe.getOwnerName(), ppe.getOwnerID(), ppe.getName(), status, ppe.getPrice(),
                ppe.getInventoryNumber(), ppe.getStartUseDate(), ppe.getLifeTime(), ppe.getSubsidiary());
        String newPPEJSON = genson.serialize(newPPE);
        stub.putStringState(inventoryNumber, newPPEJSON);

        return newPPE;
    }

    /**
     * Retrieves all ppes from the ledger.
     *
     * @param ctx the transaction context
     * @return array of ppes found on the ledger
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllPPEs(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        List<PPE> queryResults = new ArrayList<PPE>();

        QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");

        for (KeyValue result: results) {
            PPE ppe = genson.deserialize(result.getStringValue(), PPE.class);
            queryResults.add(ppe);
            System.out.println(ppe.toString());
        }

        final String response = genson.serialize(queryResults);

        return response;
    }
}

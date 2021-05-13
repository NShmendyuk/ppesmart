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
                        email = "Shmendyuk.NV@gazprom-neft.ru")))
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
    public void InitTestLedger(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        CreatePPE(ctx, "ppe1", "Иван Иванов", "001",
                "БЕЛЬЕ НАТ. ТРИКОТАЖ. 60/62 182/188", 182.85F, "720000011068",
                "10.02.2021", 20, "ГНП-Д1", "none");
        CreatePPE(ctx, "ppe2", "Петр Петрович", "002",
                "БОТИНКИ КОЖ ВЫС БЕРЦЕМ МЕТ ПОДНОСКОМ 45", 478.16F, "720000010964",
                "15.02.2021", 15, "ГНП-Д1", "none");
    }

    /**
     * Creates a new ppe on the ledger.
     *
     * @param ctx the transaction context
     * @param ppeID the ID of the new ppe
     * @param ownerName the employee name
     * @param ownerID employee's personnel number
     * @param name PPE name
     * @param price PPE price
     * @param inventoryNumber PPE inventory number in subsidiary
     * @param startUseDate a date when PPE using has been started
     * @param lifeTime a life time to use PPE
     * @param subsidiary a company owned PPE / company where the employee is located
     * @param prevSubsidiary a company where employee were previously
     * @return the created ppe
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public PPE CreatePPE(final Context ctx, final String ppeID, final String ownerName,
                           final String ownerID, final String name,
                           final Float price, final String inventoryNumber,
                           final String startUseDate, final Integer lifeTime,
                           final String subsidiary, final String prevSubsidiary) {
        ChaincodeStub stub = ctx.getStub();

        if (PPEExists(ctx, ppeID)) {
            String errorMessage = String.format("PPE %s already exists", ppeID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, PPETransferErrors.PPE_ALREADY_EXISTS.toString());
        }

        PPE PPE = new PPE(ppeID, ownerName, ownerID, name, price, inventoryNumber, startUseDate, lifeTime, subsidiary, prevSubsidiary);
        String ppeJSON = genson.serialize(PPE);
        stub.putStringState(ppeID, ppeJSON);

        return PPE;
    }

    /**
     * Retrieves an ppe with the specified ID from the ledger.
     *
     * @param ctx the transaction context
     * @param ppeID the ID of the ppe
     * @return the ppe found on the ledger if there was one
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public PPE ReadPPE(final Context ctx, final String ppeID) {
        ChaincodeStub stub = ctx.getStub();
        String ppeJSON = stub.getStringState(ppeID);

        if (ppeJSON == null || ppeJSON.isEmpty()) {
            String errorMessage = String.format("ppe %s does not exist", ppeID);
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
     * @param ppeID the ID of the ppe
     * @return the ppe found on the ledger
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ReadPPEHistory(final Context ctx, final String ppeID) {
        ChaincodeStub stub = ctx.getStub();
        QueryResultsIterator<KeyModification> ppeHistory = stub.getHistoryForKey(ppeID);

        if (ppeHistory == null) {
            String errorMessage = String.format("PPE %s history does not exist", ppeID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, PPETransferErrors.PPE_NOT_FOUND.toString());
        }
        StringBuilder infoHistoryString = new StringBuilder();
        while (ppeHistory.iterator().hasNext()) {
            KeyModification keyModification = ppeHistory.iterator().next();
            infoHistoryString.append("<").append(keyModification.getTxId()).append(":").append(keyModification.getStringValue()).append(">");
        }

        return infoHistoryString.toString();
    }

    /**
     * Updates the properties of an ppe on the ledger.
     *
     * @param ctx the transaction context
     * @param ppeID the ID of the ppe being updated
     * @param ownerID employee's personnel number being updated
     * @param price PPE price being updated
     * @param inventoryNumber PPE inventory number in subsidiary being updated
     * @param subsidiary a company owned PPE / company where the employee is located being updated
     * @param prevSubsidiary a company where employee were previously being updated
     * @return the transferred ppe
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public PPE UpdatePPE(final Context ctx, final String ppeID, final String ownerName,
                           final String ownerID, final String name,
                           final Float price, final String inventoryNumber,
                           final String startUseDate, final Integer lifeTime,
                           final String subsidiary, final String prevSubsidiary) {
        ChaincodeStub stub = ctx.getStub();

        if (!PPEExists(ctx, ppeID)) {
            String errorMessage = String.format("ppe %s does not exist", ppeID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, PPETransferErrors.PPE_NOT_FOUND.toString());
        }

        PPE newPPE = new PPE(ppeID, ownerName, ownerID, name, price, inventoryNumber, startUseDate, lifeTime, subsidiary, prevSubsidiary);
        String newppeJSON = genson.serialize(newPPE);
        stub.putStringState(ppeID, newppeJSON);
        System.out.println("Update with: " + newPPE.toString());

        return newPPE;
    }

    /**
     * Deletes ppe on the ledger.
     *
     * @param ctx the transaction context
     * @param ppeID the ID of the ppe being deleted
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void DeletePPE(final Context ctx, final String ppeID) {
        ChaincodeStub stub = ctx.getStub();

        if (!PPEExists(ctx, ppeID)) {
            String errorMessage = String.format("ppe %s does not exist", ppeID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, PPETransferErrors.PPE_NOT_FOUND.toString());
        }

        System.out.println("ppe {" + stub.getStringArgs().toString() + "} deleted");
        stub.delState(ppeID);
    }

    /**
     * Checks the existence of the ppe on the ledger
     *
     * @param ctx the transaction context
     * @param ppeID the ID of the ppe
     * @return boolean indicating the existence of the ppe
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean PPEExists(final Context ctx, final String ppeID) {
        ChaincodeStub stub = ctx.getStub();
        String ppeJSON = stub.getStringState(ppeID);

        return (ppeJSON != null && !ppeJSON.isEmpty());
    }

    /**
     * Changes the owner of a ppe on the ledger.
     *
     * @param ctx the transaction context
     * @param ppeID the ID of the ppe being transferred
     * @param newOwnerID new employee's personnel number for another company
     * @param newInventoryNumber new inventory number for another company
     * @param fromSubsidiary the company from which the employee is transferred
     * @param toSubsidiary the company to which the employee is transferred
     * @return the updated ppe
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public PPE TransferPPE(final Context ctx, final String ppeID,
                             final String newOwnerID, final String newInventoryNumber,
                             final String fromSubsidiary, final String toSubsidiary) {
        ChaincodeStub stub = ctx.getStub();
        String ppeJSON = stub.getStringState(ppeID);

        if (ppeJSON == null || ppeJSON.isEmpty()) {
            String errorMessage = String.format("ppe %s does not exist", ppeID);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, PPETransferErrors.PPE_NOT_FOUND.toString());
        }

        PPE PPE = genson.deserialize(ppeJSON, PPE.class);

        PPE newPPE = new PPE(PPE.getppeID(), PPE.getOwnerName(), newOwnerID, PPE.getName(), PPE.getPrice(),
                newInventoryNumber, PPE.getStartUseDate(), PPE.getLifeTime(), toSubsidiary, fromSubsidiary);
        String newppeJSON = genson.serialize(newPPE);
        stub.putStringState(ppeID, newppeJSON);

        return newPPE;
    }

    /**
     * Retrieves all ppes from the ledger.
     *
     * @param ctx the transaction context
     * @return array of ppes found on the ledger
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAllPPEs(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        List<PPE> queryResults = new ArrayList<PPE>();

        QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");

        for (KeyValue result: results) {
            PPE PPE = genson.deserialize(result.getStringValue(), PPE.class);
            queryResults.add(PPE);
            System.out.println(PPE.toString());
        }

        final String response = genson.serialize(queryResults);

        return response;
    }
}

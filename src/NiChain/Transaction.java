package NiChain;

import java.security.*;
import java.util.ArrayList;

public class Transaction {

    public String transactionId; // Contains a unique hash of this transaction
    public PublicKey sender; // Sender's address/public key
    public PublicKey recipient; // Recipient's address/public key
    public float value; // Amount to send to the recipient
    public byte[] signature; // Signature to prevent unauthorized transactions

    public ArrayList<TransactionInput> inputs = new ArrayList<TransactionInput>(); // Inputs for this transaction
    public ArrayList<TransactionOutput> outputs = new ArrayList<TransactionOutput>(); // Outputs for this transaction

    private static int sequence = 0; // Keeps track of the number of transactions for unique hashes

    // Constructor to initialize transaction details
    public Transaction(PublicKey from, PublicKey to, float value, ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
    }

    // Processes the transaction: Verifies, checks validity, and updates UTXOs
    public boolean processTransaction() {

        // Check if the signature is valid
        if (!verifySignature()) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

        // Retrieve UTXOs (Unspent Transaction Outputs) referenced by this transaction
        for (TransactionInput i : inputs) {
            i.UTXO = NiChain.UTXOs.get(i.transactionOutputId);
        }

        // Validate transaction inputs
        if (getInputsValue() < NiChain.minimumTransaction) {
            System.out.println("Transaction Inputs too small: " + getInputsValue());
            System.out.println("Please enter an amount greater than " + NiChain.minimumTransaction);
            return false;
        }

        // Generate transaction outputs
        float leftOver = getInputsValue() - value; // Calculate change after transaction
        transactionId = calculateHash(); // Generate unique transaction ID

        // Create outputs: sending value to recipient and returning leftover to sender
        outputs.add(new TransactionOutput(this.recipient, value, transactionId));
        outputs.add(new TransactionOutput(this.sender, leftOver, transactionId));

        // Add outputs to the list of unspent transactions
        for (TransactionOutput o : outputs) {
            NiChain.UTXOs.put(o.id, o);
        }

        // Remove used inputs from the UTXO list
        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue; // If transaction not found, skip it
            NiChain.UTXOs.remove(i.UTXO.id);
        }

        return true;
    }

    // Calculates the total value of inputs (sum of unspent transactions)
    public float getInputsValue() {
        float total = 0;
        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue; // Skip missing transactions
            total += i.UTXO.value;
        }
        return total;
    }

    // Generates a signature for the transaction using the sender's private key
    public void generateSignature(PrivateKey privateKey) {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + Float.toString(value);
        signature = StringUtil.applyECDSASig(privateKey, data);
    }

    // Verifies the transaction's signature to ensure it was signed by the sender
    public boolean verifySignature() {
        String data = StringUtil.getStringFromKey(sender) + StringUtil.getStringFromKey(recipient) + Float.toString(value);
        return StringUtil.verifyECDSASig(sender, data, signature);
    }

    // Calculates the total value of transaction outputs
    public float getOutputsValue() {
        float total = 0;
        for (TransactionOutput o : outputs) {
            total += o.value;
        }
        return total;
    }

    // Creates a unique transaction hash to prevent duplicate transactions
    private String calculateHash() {
        sequence++; // Increment sequence to ensure uniqueness
        return StringUtil.applySha256(
                StringUtil.getStringFromKey(sender) +
                        StringUtil.getStringFromKey(recipient) +
                        Float.toString(value) + sequence
        );
    }
}

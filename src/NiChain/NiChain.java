package NiChain;

import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List; // Added missing import for List
import java.util.LinkedList; // Added missing import for LinkedList

public class NiChain {

    public static ArrayList<Block> blockchain = new ArrayList<Block>(); // Stores the blockchain
    public static HashMap<String, TransactionOutput> UTXOs = new HashMap<String, TransactionOutput>(); // Stores unspent transactions

    public static int difficulty = 3; // Mining difficulty level
    public static float minimumTransaction = 0.1f; // Minimum transaction amount
    public static Wallet walletA;
    public static Wallet walletB;
    public static Transaction genesisTransaction; // First transaction (Genesis transaction)

    public static void main(String[] args) {
        // Adds security provider for cryptographic operations
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // Create wallets
        walletA = new Wallet();
        walletB = new Wallet();
        Wallet coinbase = new Wallet();

        // Create the Genesis transaction, sending 100 coins from the system (coinbase) to walletA
        genesisTransaction = new Transaction(coinbase.publicKey, walletA.publicKey, 100f, null);
        genesisTransaction.generateSignature(coinbase.privateKey);
        genesisTransaction.transactionId = "0"; // Manually setting genesis transaction ID

        // Add the first UTXO (unspent transaction output) to the system
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.recipient, genesisTransaction.value, genesisTransaction.transactionId));
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        System.out.println("Creating and Mining Genesis block...");
        Block genesis = new Block("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);

        // Transaction: WalletA → WalletB (40 coins)
        Block block1 = new Block(genesis.hash);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("\nWalletA is Attempting to send funds (40) to WalletB...");
        block1.addTransaction(walletA.sendFunds(walletB.publicKey, 40f));
        addBlock(block1);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        // Transaction: WalletA → WalletB (1000 coins) (should fail due to insufficient funds)
        Block block2 = new Block(block1.hash);
        System.out.println("\nWalletA Attempting to send more funds (1000) than it has...");
        block2.addTransaction(walletA.sendFunds(walletB.publicKey, 1000f));
        addBlock(block2);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        // Transaction: WalletB → WalletA (20 coins)
        Block block3 = new Block(block2.hash);
        System.out.println("\nWalletB is Attempting to send funds (20) to WalletA...");
        block3.addTransaction(walletB.sendFunds(walletA.publicKey, 20));
        addBlock(block3);
        System.out.println("\nWalletA's balance is: " + walletA.getBalance());
        System.out.println("WalletB's balance is: " + walletB.getBalance());

        // Validate blockchain integrity
        isChainValid();
    }

    // Validates the blockchain by checking hashes, transactions, and UTXOs
    public static Boolean isChainValid() {
        Block currentBlock;
        Block previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String, TransactionOutput> tempUTXOs = new HashMap<String, TransactionOutput>();

        // Store the genesis transaction output
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        for (int i = 1; i < blockchain.size(); i++) {
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i - 1);

            // Verify the block's hash
            if (!currentBlock.hash.equals(currentBlock.calculateHash())) {
                System.out.println("#Current Hashes not equal");
                return false;
            }

            // Verify the previous block hash reference
            if (!previousBlock.hash.equals(currentBlock.previousHash)) {
                System.out.println("#Previous Hashes not equal");
                return false;
            }

            // Check if the block has been mined
            if (!currentBlock.hash.substring(0, difficulty).equals(hashTarget)) {
                System.out.println("#This block hasn't been mined");
                return false;
            }

            // Validate all transactions in the block
            for (int t = 0; t < currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                // Check transaction signature
                if (!currentTransaction.verifySignature()) {
                    System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                    return false;
                }

                // Verify transaction input/output values
                if (currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Inputs are not equal to outputs on Transaction(" + t + ")");
                    return false;
                }

                // Validate transaction inputs
                for (TransactionInput input : currentTransaction.inputs) {
                    TransactionOutput tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if (tempOutput == null) {
                        System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                        return false;
                    }

                    if (input.UTXO.value != tempOutput.value) {
                        System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                // Add transaction outputs to temporary UTXO list
                for (TransactionOutput output : currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                // Ensure transaction recipients match the intended addresses
                if (currentTransaction.outputs.get(0).reciepient != currentTransaction.recipient) {
                    System.out.println("#Transaction(" + t + ") output recipient is incorrect");
                    return false;
                }
                if (currentTransaction.outputs.size() > 1 && currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                    System.out.println("#Transaction(" + t + ") output 'change' is not sent back to sender.");
                    return false;
                }
            }
        }
        System.out.println("Blockchain is valid");
        return true;
    }

    // Adds a new block to the blockchain
    public static void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
}

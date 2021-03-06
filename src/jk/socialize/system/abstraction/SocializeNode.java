/**
 * @author Joshua Kissoon
 * @date 20131119
 * @desc A node class that is an extension of the Likir Node to provide Socialize functionality
 */
package jk.socialize.system.abstraction;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import jk.socialize.system.core.content.ConnectionRequests;
import jk.socialize.system.core.content.SocializeContent;
import unito.likir.Node;
import unito.likir.NodeId;
import unito.likir.io.ObservableFuture;
import unito.likir.storage.StorageEntry;

public class SocializeNode extends Node
{

    /**
     * @desc Method to call the Likir Node constructor
     * @param userId
     */
    public SocializeNode(String userId)
    {
        super(userId);
    }

    /**
     * @desc Method to call the Likir Node constructor
     * @param f
     *
     * @throws IOException
     */
    public SocializeNode(File f) throws IOException
    {
        super(f);
    }

    /**
     * @desc Store a value locally
     * @return The task that is being ran to store the content locally
     *
     * @param content The SocializeContent content to store
     *
     * @throws IOException
     */
    public Boolean storeLocally(SocializeContent content) throws IOException
    {
        StorageEntry entry = getEntryFactory().buildStorageEntry(content.getKey(), content.getValue(), content.getType(), content.getTtl());

        Boolean success = getStorage().store(entry);
        System.out.println("Storing content locally; Success: " + success);
        return success;
    }

    /**
     * @desc Method that puts a socialize content by calling the Likir Node put method
     * @return
     *
     * @param content Some socialize content to put on the network
     */
    public synchronized ObservableFuture<Integer> put(SocializeContent content)
    {
        return put(content.getKey(), content.getValue(), content.getType(), content.getTtl());
    }

    /**
     * @desc A method that stores content both on the DHT and within this node's local storage
     * @param content The content to be stored
     *
     * @return The number of replicas at which the content was stored
     *
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public Integer storeLocallyAndUniversally(SocializeContent content) throws IOException, InterruptedException, ExecutionException
    {
        this.storeLocally(content);
        System.out.println("Local Storage Successful");
        Integer replicas = this.put(content).get();
        System.out.println("Foreign Storage Successful at " + replicas + " replicas");
        return replicas;
    }

    public SocializeContent getContent(NodeId contentId, String ownerUid, SocializeContent content)
    {

        /* We need to load the connection requests */
        try
        {
            System.out.println("Node \"" + this.getUserId() + "\" Loading Connections request object \n");

            /* Get 5 of this user's profile and choose the most recent */
            Collection<StorageEntry> results = this.get(contentId, content.getType(), ownerUid, true, 5).get();

            if (results.size() > 0)
            {
                long recency = 0;
                for (StorageEntry e : results) //print the found values
                {
                    if (e.getSubmissionTime() > recency)
                    {
                        recency = e.getSubmissionTime();

                        /* Load/update the content from this entry */
                        content.loadData(e.getContent().getValue());
                    }
                }
            }
        }
        catch (InterruptedException | ExecutionException ie)
        {
            System.err.println("Posts References loading Interrupted");
        }

        return content;
    }

}

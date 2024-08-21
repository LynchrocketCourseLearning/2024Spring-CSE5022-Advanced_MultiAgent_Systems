package marsExplorers;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.StreamSupport;

import marsExplorers.Message.MessageType;
import repast.simphony.context.Context;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.space.graph.Network;
import repast.simphony.util.ContextUtils;

public class MessageManager {
    private Explorer explorer;

    /**
     * the ratio in communicating with others
     */
    private int communicateRatio;
    private Queue<Message> messageQueue;

    public MessageManager(Explorer explorer, int communicateRatio) {
        this.explorer = explorer;
        this.communicateRatio = communicateRatio;
        this.messageQueue = new LinkedList<>();
    }

    public void sendMessage(Explorer recipient, Message message) {
        recipient.receiveMessage(message);
    }

    public void receiveMessage(Message message) {
        if (message.getType() == MessageType.NORMAL_EXPIRED) {
            this.messageQueue.removeIf(msg -> msg.getContent().equals(message.getContent()));
        } else {
            this.messageQueue.add(message);
        }
    }

    public void broadcastMessage(Message message, boolean onlyGroup) {
        List<GridCell<Explorer>> nghCells = explorer.getThisGridNeighbour(Explorer.class, true, communicateRatio,
                communicateRatio);
        for (GridCell<Explorer> cell : nghCells) {
            if (cell.size() > 0) {
                List<Explorer> explorersAround = StreamSupport.stream(cell.items().spliterator(), false)
                        .filter(otherExplorer -> otherExplorer != explorer).toList();
                for (Explorer otherExplorer : explorersAround) {
                    // send only to explorers in same group
                    if (onlyGroup && explorer.getGroupId() == otherExplorer.getGroupId()) {
                        sendMessage(explorer, message);
                    }
                    if (!onlyGroup) {
                        sendMessage(explorer, message);
                    }
                }
            }
        }
    }

    public void processMessages() {
        Context<Object> context = ContextUtils.getContext(explorer);
        Network<Object> net = (Network<Object>) context.getProjection("agents_network");
        while (!messageQueue.isEmpty()) {
            Message message = messageQueue.poll();
            switch (message.getType()) {
                case MATERIAL_FOUND -> explorer.addMaterialSpot(message.getContent());
                case DEST_FOUND -> explorer.addDestination(message.getContent());
                case NEED_HELP -> {
                    if (!explorer.nearThanDest(message.getContent())) {
                        explorer.setNextTargetPoint(message.getContent());
                    }
                }
                case FINDING_GROUP -> {
                    if (!explorer.isInGroup() && !message.getSender().isInGroup()) {
                        explorer.setInGroup(true);
                        message.getSender().setInGroup(true);
                        explorer.setGroupId(message.getSender().getGroupId());
                        net.addEdge(explorer, message.getSender(), 0.0);
                    }
                }
                default -> {
                }
            }
            System.out.println("Processed message: " + message.getContent() +
                    "; type: " + message.getType() +
                    "; from: " + message.getSender().getId());
        }
    }
}

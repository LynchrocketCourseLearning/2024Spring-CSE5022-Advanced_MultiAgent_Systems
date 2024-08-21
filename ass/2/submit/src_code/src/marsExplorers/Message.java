package marsExplorers;

import repast.simphony.space.grid.GridPoint;

public class Message {
	private Explorer sender;
	private GridPoint pt;
	private MessageType type;

	public Message(Explorer sender, GridPoint pt, MessageType type) {
		this.sender = sender;
		this.pt = pt;
		this.type = type;
	}

	public Explorer getSender() {
		return this.sender;
	}

	public GridPoint getContent() {
		return this.pt;
	}

	public MessageType getType() {
		return this.type;
	}

	public enum MessageType {
		MATERIAL_FOUND,
		DEST_FOUND,
		NEED_HELP,
		NORMAL_EXPIRED,
		
		FINDING_GROUP,
	}
}

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.connector.manual.itsm.ws;

public enum TicketStatus {

	// from Remedy documentation:
	//Assigned - This indicates the ticket has been assigned to the agency submitting the ticket or the AZNet II Help Desk.  If the submitting agency wishes to add more information or hold the ticket for any reason, this keeps control with the submitter allowing the input of more information or cancellation.
	//New - When a ticket is first created and saved, its initial status will be New until it is assigned or canceled
	//On Hold - Ticket is currently not able to be ‘worked’ due to factors preventing its resolution or closure.
	//In Progress -The ticket has been submitted and is being worked on by the group to which it is assigned
	//Canceled - The ticket was cancelled due to an incorrect ticket type (another ticket is reopened correctly), or any other reason the ticket does not need to be worked   Agencies can cancel the ticket it if has not been assigned to the AZNetII helpdesk or CenturyLink.
	//Resolved - The ticket has been resolved and no further action is required.  This status will automatically change to closed unless the ticket is reopened by CenturyLink because of outstanding issues.
	//Closed - After remaining in a resolved state for 5 days (the required waiting period) without being reopened, a ticket is automatically closed by the system.

	NEW("0"),
	ASSIGNED("1"),
	IN_PROGRESS("2"),
	PENDING("3"),
	RESOLVED("4"),
	CLOSED("5"),
	CANCELLED("6");
	
	private String code;
	
	private TicketStatus(String code) {
		this.code = code;
	}

	public String getCode() {
		TicketStatus.values();
		return code;
	}
}

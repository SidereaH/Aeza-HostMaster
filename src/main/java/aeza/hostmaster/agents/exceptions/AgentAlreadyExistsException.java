package aeza.hostmaster.agents.exceptions;


public class AgentAlreadyExistsException extends RuntimeException {
    public AgentAlreadyExistsException(String message) { super(message); }
}
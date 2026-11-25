package ee.taltech.ite4120.guidelines.approverregistry;

/**
 * The person who approved a guideline, in OUR domain vocabulary — English
 * names, our shape. The registry's own field names live in the wire records
 * and never leak past the mapper.
 */
public record ApproverInfo(String isikukood, String firstName, String lastName) {}

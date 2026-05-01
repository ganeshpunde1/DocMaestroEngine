package com.maestro.po.ms.inference.exception;

/**
 * Exception thrown when an error occurs during AWS Bedrock Guardrail inference
 * operations.
 * <p>
 * This exception is typically used to indicate issues such as missing
 * configuration, invalid input, or violations detected by Guardrails during
 * inference. It includes an error code and a descriptive error message to aid
 * in debugging and logging.
 * </p>
 *
 * @author Ganesh Punde
 * @version 1.0
 */
public class InferenceGuardrailViolationException extends RuntimeException {
	private static final long serialVersionUID = -2898994084626952363L;

	/** Error code representing the specific type of error. */
	private final String errorCode;

	/** Detailed error message describing the exception. */
	private final String errorMessage;

	/**
	 * Constructs a new InferenceGuardrailViolationException with the specified
	 * error code and message.
	 *
	 * @param errorCode    the numeric code representing the error
	 * @param errorMessage the detailed message explaining the error
	 */
	public InferenceGuardrailViolationException(String errorCode, String errorMessage) {
		super(errorMessage); // Sets the message in the base Exception class
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	/**
	 * Returns the error code associated with this exception.
	 *
	 * @return the error code
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Returns the error message associated with this exception.
	 *
	 * @return the error message
	 */
	@Override
	public String getMessage() {
		return errorMessage;
	}

	/**
	 * Returns a string representation of the InferenceGuardrailViolationException.
	 *
	 * @return a string containing the error code and message
	 */
	@Override
	public String toString() {
		return "InferenceGuardrailViolationException{" + "errorCode=" + errorCode + ", errorMessage='" + errorMessage
				+ '\'' + '}';
	}
}

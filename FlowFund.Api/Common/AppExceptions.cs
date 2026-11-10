namespace FlowFund.Api.Common
{

    // Three small, purpose-specific exceptions so GroupService can signal *why*
    // an operation failed, and GroupsController can map each one to the correct
    // HTTP status code (404 / 403 / 409) without a big if/else chain of message-string checks.
    public class NotFoundException : Exception
    {
        public NotFoundException(string message) : base(message) { }
    }

    public class ForbiddenException : Exception
    {
        public ForbiddenException(string message) : base(message) { }
    }

    public class ConflictException : Exception
    {
        public ConflictException(string message) : base(message) { }
    }

    // 400: the request itself is invalid (bad amount, empty id, ...).
    public class BadRequestException : Exception
    {
        public BadRequestException(string message) : base(message) { }
    }

    // 401: credentials presented but not acceptable (e.g. an invalid refresh token).
    public class UnauthorizedException : Exception
    {
        public UnauthorizedException(string message) : base(message) { }
    }
}

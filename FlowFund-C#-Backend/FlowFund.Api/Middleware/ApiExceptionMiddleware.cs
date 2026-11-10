using System.Diagnostics;
using System.Security.Claims;
using FlowFund.Api.Common;

namespace FlowFund.Api.Middleware
{
    /// <summary>
    /// One place that (1) logs every request and (2) turns the service-layer exceptions into
    /// HTTP status codes. Principle: separation of concerns - services decide WHAT went wrong,
    /// this middleware decides HOW that is reported over HTTP.
    /// Never logs bodies or the Authorization header, so tokens cannot leak into logs.
    /// </summary>
    public class ApiExceptionMiddleware
    {
        private const string Tag = "ApiRequest";
        private readonly RequestDelegate _next;
        private readonly ILogger<ApiExceptionMiddleware> _log;

        public ApiExceptionMiddleware(RequestDelegate next, ILogger<ApiExceptionMiddleware> log)
        {
            _next = next;
            _log = log;
        }

        public async Task InvokeAsync(HttpContext context)
        {
            var sw = Stopwatch.StartNew();
            try
            {
                await _next(context);
            }
            catch (Exception ex) when (!context.Response.HasStarted)
            {
                var (status, message) = Map(ex);
                if (status == StatusCodes.Status500InternalServerError)
                {
                    _log.LogError(ex, "{Tag}: unhandled error on {Method} {Path}", Tag, context.Request.Method, context.Request.Path);
                }
                else
                {
                    _log.LogWarning("{Tag}: {Status} on {Method} {Path} - {Message}", Tag, status, context.Request.Method, context.Request.Path, ex.Message);
                }

                context.Response.StatusCode = status;
                context.Response.ContentType = "application/json";
                await context.Response.WriteAsJsonAsync(new { error = message });
            }
            finally
            {
                sw.Stop();
                var user = context.User.FindFirstValue(ClaimTypes.NameIdentifier) ?? context.User.FindFirstValue("sub") ?? "anonymous";
                _log.LogInformation("{Tag}: {Method} {Path} -> {Status} in {Ms}ms (user {User})",
                    Tag, context.Request.Method, context.Request.Path, context.Response.StatusCode, sw.ElapsedMilliseconds, user);
            }
        }

        /// <summary>Pure mapping, public static so a unit test can call it directly.</summary>
        public static (int Status, string Message) Map(Exception ex) => ex switch
        {
            NotFoundException e => (StatusCodes.Status404NotFound, e.Message),
            ForbiddenException e => (StatusCodes.Status403Forbidden, e.Message),
            ConflictException e => (StatusCodes.Status409Conflict, e.Message),
            BadRequestException e => (StatusCodes.Status400BadRequest, e.Message),
            UnauthorizedException e => (StatusCodes.Status401Unauthorized, e.Message),
            // Never leak internals of unexpected errors to the client.
            _ => (StatusCodes.Status500InternalServerError, "An unexpected error occurred.")
        };
    }
}

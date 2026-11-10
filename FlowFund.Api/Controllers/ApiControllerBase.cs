using System.Security.Claims;
using FlowFund.Api.Common;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FlowFund.Api.Controllers
{
    /// <summary>Shared base: every endpoint needs a JWT, and the caller's id comes ONLY from the token.</summary>
    [ApiController]
    [Authorize]
    public abstract class ApiControllerBase : ControllerBase
    {
        protected int CurrentUserId =>
            int.TryParse(User.FindFirstValue(ClaimTypes.NameIdentifier) ?? User.FindFirstValue("sub"), out var id)
                ? id
                : throw new UnauthorizedException("Token does not contain a valid user id.");
    }
}

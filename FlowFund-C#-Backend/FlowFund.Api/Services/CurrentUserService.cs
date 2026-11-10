using System.Security.Claims;
using FlowFund.Api.Common;
using FlowFund.Api.Data;
using FlowFund.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace FlowFund.Api.Services
{
   
    /// Resolves the signed-in user for controllers that talk to the database directly.
    /// The API authenticates with ITS OWN JWT (issued by /auth/sso/verify), whose "sub" claim is the internal
    /// User.Id. The user row is created at sign-in, so nothing is auto-provisioned here any more: a token whose
    /// user no longer exists is rejected with 401. (The old version read the claim as a Firebase UID, which can
    /// never match a "sub" holding a database id.)
   
    public class CurrentUserService
    {
        private readonly FlowFundDbContext _db;
        private readonly IHttpContextAccessor _httpContextAccessor;

        public CurrentUserService(FlowFundDbContext db, IHttpContextAccessor httpContextAccessor)
        {
            _db = db;
            _httpContextAccessor = httpContextAccessor;
        }

        // Name kept so existing controllers keep compiling.
        public async Task<User> GetOrCreateCurrentUserAsync()
        {
            var principal = _httpContextAccessor.HttpContext?.User
                ?? throw new UnauthorizedException("No authenticated user in context.");

            // JwtBearer may expose "sub" as ClaimTypes.NameIdentifier or keep it as "sub"; accept both.
            var raw = principal.FindFirstValue(ClaimTypes.NameIdentifier) ?? principal.FindFirstValue("sub");
            if (!int.TryParse(raw, out var userId))
            {
                throw new UnauthorizedException("Token does not contain a valid user id.");
            }

            return await _db.Users.FirstOrDefaultAsync(u => u.Id == userId)
                ?? throw new UnauthorizedException("User no longer exists.");
        }
    }
}

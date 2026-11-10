using FlowFund.Api.Common;
using FlowFund.Api.Data;
using FlowFund.Api.Dtos;
using FlowFund.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace FlowFund.Api.Services
{
    // This is the "verify token, create or fetch the matching User row, return
    // a JWT access/refresh pair" logic the brief describes for /auth/sso/verify.
    // Depends on ITokenVerifier and IJwtService (interfaces, not concrete Firebase/JWT
    // code) specifically so AuthServiceTests can substitute mocks for both.
    public class AuthService : IAuthService
    {
        private readonly FlowFundDbContext _db;
        private readonly ITokenVerifier _tokenVerifier;
        private readonly IJwtService _jwtService;

        private const string Tag = "AuthService";
        private readonly ILogger<AuthService> _log;

        public AuthService(FlowFundDbContext db, ITokenVerifier tokenVerifier, IJwtService jwtService,
            ILogger<AuthService>? log = null)
        {
            _db = db;
            _tokenVerifier = tokenVerifier;
            _jwtService = jwtService;
            // Optional so the Part 2 tests, which construct AuthService with three arguments, still compile.
            _log = log ?? Microsoft.Extensions.Logging.Abstractions.NullLogger<AuthService>.Instance;
        }

        public async Task<AuthResponseDto> VerifyAndIssueTokensAsync(string idToken)
        {
            // Step 1: verify with Firebase — throws FirebaseAuthException on a bad
            // token, which we deliberately do NOT catch here; AuthController catches
            // it, since "how to respond over HTTP" is a controller concern, not a
            // service concern.
            var verified = await _tokenVerifier.VerifyAsync(idToken);

            // Step 2: find the matching User by FirebaseUid (the field we added
            // specifically to make this lookup possible).
            var user = await _db.Users.FirstOrDefaultAsync(u => u.FirebaseUid == verified.Uid);

            // Step 3: first-time login — create the User row. This is the
            // "create or fetch" half of the brief's description.
            if (user is null)
            {
                user = new User
                {
                    FirebaseUid = verified.Uid,
                    Email = verified.Email ?? string.Empty,
                    FullName = verified.Name ?? string.Empty,
                    CreatedAt = DateTime.UtcNow,
                    Currency = "ZAR" // REQ-SET-02 default
                };

                _db.Users.Add(user);
                await _db.SaveChangesAsync();
                _log.LogInformation("{Tag}: created user {UserId} on first SSO login", Tag, user.Id);
            }
            else
            {
                _log.LogInformation("{Tag}: SSO login for existing user {UserId}", Tag, user.Id);
            }

            // Step 4: issue our own JWTs — the app authenticates to *our* API with
            // these from now on, not with the Firebase ID token directly.
            return BuildResponse(user);
        }

        /// POST /api/v1/auth/refresh. Validates a refresh token (signature, expiry AND
        /// token_use == "refresh"), re-reads the user, and issues a brand-new pair.
        /// Rotating the refresh token on every use limits how long a stolen one stays useful.

        public async Task<AuthResponseDto> RefreshAsync(string refreshToken)
        {
            var principal = _jwtService.ValidateRefreshToken(refreshToken);
            var sub = principal?.FindFirst("sub")?.Value;

            if (principal is null || !int.TryParse(sub, out var userId))
            {
                _log.LogWarning("{Tag}: refresh rejected - token invalid, expired or not a refresh token", Tag);
                throw new UnauthorizedException("Invalid or expired refresh token.");
            }

            var user = await _db.Users.FirstOrDefaultAsync(u => u.Id == userId);
            if (user is null)
            {
                _log.LogWarning("{Tag}: refresh rejected - user {UserId} no longer exists", Tag, userId);
                throw new UnauthorizedException("Invalid or expired refresh token.");
            }

            _log.LogInformation("{Tag}: refresh succeeded for user {UserId}", Tag, userId);
            return BuildResponse(user);
        }

        private AuthResponseDto BuildResponse(User user)
        {
            var tokens = _jwtService.GenerateTokens(user.Id, user.Email);
            return new AuthResponseDto
            {
                AccessToken = tokens.AccessToken,
                RefreshToken = tokens.RefreshToken,
                User = UserMapper.ToDto(user)
            };
        }
    }
}

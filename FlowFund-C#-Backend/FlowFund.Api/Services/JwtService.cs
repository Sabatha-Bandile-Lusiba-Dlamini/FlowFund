using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using Microsoft.IdentityModel.Tokens;

namespace FlowFund.Api.Services
{
    // Structured following the official JWT bearer docs at
    // that page documents the *validation* side; this class is the matching
    // *issuing* side using the same signing key/issuer/audience so the two line up.
    public class JwtService : IJwtService
    {
        private readonly IConfiguration _config;

        public JwtService(IConfiguration config)
        {
            _config = config;
        }

        public TokenPair GenerateTokens(int userId, string email)
        {
            // Short-lived access token for normal API calls, long-lived refresh
            // token for getting a new access token later without re-doing the
            // full Firebase SSO flow every 15 minutes.
            var accessToken = GenerateToken(userId, email, TimeSpan.FromMinutes(15), isRefresh: false);
            var refreshToken = GenerateToken(userId, email, TimeSpan.FromDays(30), isRefresh: true);

            return new TokenPair { AccessToken = accessToken, RefreshToken = refreshToken };
        }


        /// Validates with the SAME issuer/audience/key used for issuing. MapInboundClaims=false keeps
        /// the claim name "sub" instead of remapping it to ClaimTypes.NameIdentifier.
        /// Returns null (never throws) for any bad token, so callers have one failure path.

        public System.Security.Claims.ClaimsPrincipal? ValidateRefreshToken(string token)
        {
            var handler = new JwtSecurityTokenHandler { MapInboundClaims = false };
            try
            {
                var principal = handler.ValidateToken(token, new TokenValidationParameters
                {
                    ValidateIssuer = true,
                    ValidateAudience = true,
                    ValidateLifetime = true,
                    ValidateIssuerSigningKey = true,
                    ValidIssuer = _config["Jwt:Issuer"],
                    ValidAudience = _config["Jwt:Audience"],
                    IssuerSigningKey = new SymmetricSecurityKey(Convert.FromBase64String(_config["Jwt:Secret"]!)),
                    ClockSkew = TimeSpan.FromMinutes(1)
                }, out _);

                return principal.FindFirst("token_use")?.Value == "refresh" ? principal : null;
            }
            catch (Exception ex) when (ex is SecurityTokenException or ArgumentException)
            {
                return null;
            }
        }

        private string GenerateToken(int userId, string email, TimeSpan lifetime, bool isRefresh)
        {
            var keyBytes = Convert.FromBase64String(_config["Jwt:Secret"]!);
            var key = new SymmetricSecurityKey(keyBytes);
            var creds = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);

            var claims = new List<Claim>
            {
                // "sub" is the standard JWT claim for "who is this token about" —
                // ASP.NET Core's JwtBearer handler maps it to ClaimTypes.NameIdentifier
                // by default, which is what GroupsController.CurrentUserId reads.
                new Claim(JwtRegisteredClaimNames.Sub, userId.ToString()),
                new Claim(JwtRegisteredClaimNames.Email, email),

                // A unique ID per token — standard practice that also leaves room
                // for a future revocation list, even though nothing checks it yet.
                new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString()),

                // Custom claim distinguishing access vs refresh tokens, so a
                // refresh endpoint (not built yet — out of this round's scope)
                // can reject someone trying to use an access token to refresh.
                new Claim("token_use", isRefresh ? "refresh" : "access")
            };

            var token = new JwtSecurityToken(
                issuer: _config["Jwt:Issuer"],
                audience: _config["Jwt:Audience"],
                claims: claims,
                expires: DateTime.UtcNow.Add(lifetime),
                signingCredentials: creds
            );

            return new JwtSecurityTokenHandler().WriteToken(token);
        }
    }
}

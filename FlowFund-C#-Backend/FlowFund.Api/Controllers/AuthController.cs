using FirebaseAdmin.Auth;
using FlowFund.Api.Dtos;
using FlowFund.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace FlowFund.Api.Controllers
{
    // Route matches the brief exactly: POST /api/v1/auth/sso/verify
    [ApiController]
    [Route("api/v1/auth")]
    public class AuthController : ControllerBase
    {
        private readonly IAuthService _authService;

        // Constructor injection 
        //the controller asks for IAuthService, and Program.cs's
        // AddScoped<IAuthService, AuthService>() decides what it actually gets.
        public AuthController(IAuthService authService)
        {
            _authService = authService;
        }

        /// <summary>POST api/v1/auth/refresh - anonymous by design: the refresh token IS the credential.</summary>
        [HttpPost("refresh")]
        public async Task<ActionResult<AuthResponseDto>> Refresh([FromBody] RefreshRequest request)
        {
            if (string.IsNullOrWhiteSpace(request.RefreshToken))
            {
                return BadRequest("refreshToken is required.");
            }

            // UnauthorizedException is mapped to 401 by ApiExceptionMiddleware.
            return Ok(await _authService.RefreshAsync(request.RefreshToken));
        }

        [HttpPost("sso/verify")]
        public async Task<ActionResult<AuthResponseDto>> VerifySso([FromBody] SsoVerifyRequest request)
        {
            if (string.IsNullOrWhiteSpace(request.IdToken))
            {
                return BadRequest("idToken is required.");
            }

            try
            {
                var result = await _authService.VerifyAndIssueTokensAsync(request.IdToken);
                return Ok(result);
            }
            catch (FirebaseAuthException)
            {
                // This is the "how do we respond over HTTP" decision that belongs
                // in the controller, not in AuthService — see the note in AuthService.cs.
                return Unauthorized("Invalid or expired ID token.");
            }
        }
    }
}

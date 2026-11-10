using System.IdentityModel.Tokens.Jwt;
using FlowFund.Api.Common;
using FlowFund.Api.Middleware;
using FlowFund.Api.Services;
using Microsoft.Extensions.Configuration;
using Moq;
using Xunit;

namespace FlowFund.Api.Tests
{
    public class AuthRefreshTests
    {
        private static JwtService NewJwt(string? secretBase64 = null)
        {
            var cfg = new Mock<IConfiguration>();
            cfg.Setup(c => c["Jwt:Secret"]).Returns(secretBase64 ?? Convert.ToBase64String(Enumerable.Repeat((byte)7, 32).ToArray()));
            cfg.Setup(c => c["Jwt:Issuer"]).Returns("flowfund-test");
            cfg.Setup(c => c["Jwt:Audience"]).Returns("flowfund-app");
            return new JwtService(cfg.Object);
        }

        [Fact]
        public void ValidateRefreshToken_AcceptsRealRefreshToken()
        {
            var jwt = NewJwt();
            var pair = jwt.GenerateTokens(42, "a@b.com");

            var principal = jwt.ValidateRefreshToken(pair.RefreshToken);

            Assert.NotNull(principal);
            Assert.Equal("42", principal!.FindFirst(JwtRegisteredClaimNames.Sub)!.Value);
        }

        [Fact]
        public void ValidateRefreshToken_RejectsAccessToken()
        {
            var jwt = NewJwt();
            var pair = jwt.GenerateTokens(42, "a@b.com");

            Assert.Null(jwt.ValidateRefreshToken(pair.AccessToken));
        }

        [Fact]
        public void ValidateRefreshToken_RejectsGarbageAndTokenSignedWithOtherKey()
        {
            var jwt = NewJwt();
            var foreign = NewJwt(Convert.ToBase64String(Enumerable.Repeat((byte)9, 32).ToArray())).GenerateTokens(1, "x@y.com");

            Assert.Null(jwt.ValidateRefreshToken("not-a-jwt"));
            Assert.Null(jwt.ValidateRefreshToken(foreign.RefreshToken));
        }

        [Fact]
        public async Task Refresh_ValidToken_ReturnsNewPair_ForExistingUser()
        {
            using var db = TestSupport.NewDb(nameof(Refresh_ValidToken_ReturnsNewPair_ForExistingUser));
            var user = await TestSupport.AddUserAsync(db);
            var jwt = NewJwt();
            var auth = new AuthService(db, new Mock<ITokenVerifier>().Object, jwt);
            var old = jwt.GenerateTokens(user.Id, user.Email);

            var result = await auth.RefreshAsync(old.RefreshToken);

            Assert.Equal(user.Id, result.User.Id);
            Assert.NotNull(jwt.ValidateRefreshToken(result.RefreshToken));
        }

        [Fact]
        public async Task Refresh_AccessTokenPresentedAsRefresh_ThrowsUnauthorized()
        {
            using var db = TestSupport.NewDb(nameof(Refresh_AccessTokenPresentedAsRefresh_ThrowsUnauthorized));
            var user = await TestSupport.AddUserAsync(db);
            var jwt = NewJwt();
            var auth = new AuthService(db, new Mock<ITokenVerifier>().Object, jwt);

            await Assert.ThrowsAsync<UnauthorizedException>(() =>
                auth.RefreshAsync(jwt.GenerateTokens(user.Id, user.Email).AccessToken));
        }

        [Fact]
        public async Task Refresh_DeletedUser_ThrowsUnauthorized()
        {
            using var db = TestSupport.NewDb(nameof(Refresh_DeletedUser_ThrowsUnauthorized));
            var jwt = NewJwt();
            var auth = new AuthService(db, new Mock<ITokenVerifier>().Object, jwt);

            await Assert.ThrowsAsync<UnauthorizedException>(() =>
                auth.RefreshAsync(jwt.GenerateTokens(999, "ghost@x.com").RefreshToken));
        }

        [Theory]
        [InlineData(typeof(NotFoundException), 404)]
        [InlineData(typeof(ForbiddenException), 403)]
        [InlineData(typeof(ConflictException), 409)]
        [InlineData(typeof(BadRequestException), 400)]
        [InlineData(typeof(UnauthorizedException), 401)]
        public void Middleware_MapsEachAppException_ToItsStatusCode(Type type, int expected)
        {
            var ex = (Exception)Activator.CreateInstance(type, "msg")!;
            Assert.Equal(expected, ApiExceptionMiddleware.Map(ex).Status);
        }

        [Fact]
        public void Middleware_HidesDetailsOfUnexpectedErrors()
        {
            var (status, message) = ApiExceptionMiddleware.Map(new InvalidOperationException("secret connection string"));

            Assert.Equal(500, status);
            Assert.DoesNotContain("secret", message);
        }
    }
}

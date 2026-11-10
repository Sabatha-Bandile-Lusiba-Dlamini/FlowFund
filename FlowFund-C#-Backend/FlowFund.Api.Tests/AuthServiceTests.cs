using FlowFund.Api.Data;
using FlowFund.Api.Services;
using Microsoft.EntityFrameworkCore;
using Moq;
using Xunit;

namespace FlowFund.Api.Tests
{
    public class AuthServiceTests
    {
       
        private static FlowFundDbContext CreateInMemoryDb(string name)
        {
            var options = new DbContextOptionsBuilder<FlowFundDbContext>()
                .UseInMemoryDatabase(name)
                .Options;
            return new FlowFundDbContext(options);
        }

        [Fact]
        public async Task VerifyAndIssueTokens_CreatesNewUser_WhenFirebaseUidNotFound()
        {
            // Arrange
            using var db = CreateInMemoryDb(nameof(VerifyAndIssueTokens_CreatesNewUser_WhenFirebaseUidNotFound));

            var tokenVerifierMock = new Mock<ITokenVerifier>();
            tokenVerifierMock
                .Setup(v => v.VerifyAsync("valid-token"))
                .ReturnsAsync(new VerifiedTokenInfo { Uid = "firebase-uid-1", Email = "test@flowfund.co.za", Name = "Test User" });

            var jwtServiceMock = new Mock<IJwtService>();
            jwtServiceMock
                .Setup(j => j.GenerateTokens(It.IsAny<int>(), It.IsAny<string>()))
                .Returns(new TokenPair { AccessToken = "access-token", RefreshToken = "refresh-token" });

            var authService = new AuthService(db, tokenVerifierMock.Object, jwtServiceMock.Object);

            
            var result = await authService.VerifyAndIssueTokensAsync("valid-token");

            Assert.Equal("access-token", result.AccessToken);
            Assert.Equal("test@flowfund.co.za", result.User.Email);
            Assert.Single(db.Users);
        }

        [Fact]
        public async Task VerifyAndIssueTokens_ReusesExistingUser_WhenFirebaseUidAlreadyExists()
        {
            using var db = CreateInMemoryDb(nameof(VerifyAndIssueTokens_ReusesExistingUser_WhenFirebaseUidAlreadyExists));

            db.Users.Add(new Models.User
            {
                FirebaseUid = "firebase-uid-2",
                Email = "existing@flowfund.co.za",
                FullName = "Existing User",
                CreatedAt = DateTime.UtcNow
            });
            await db.SaveChangesAsync();

            var tokenVerifierMock = new Mock<ITokenVerifier>();
            tokenVerifierMock
                .Setup(v => v.VerifyAsync("valid-token"))
                .ReturnsAsync(new VerifiedTokenInfo { Uid = "firebase-uid-2", Email = "existing@flowfund.co.za" });

            var jwtServiceMock = new Mock<IJwtService>();
            jwtServiceMock
                .Setup(j => j.GenerateTokens(It.IsAny<int>(), It.IsAny<string>()))
                .Returns(new TokenPair { AccessToken = "access-token", RefreshToken = "refresh-token" });

            var authService = new AuthService(db, tokenVerifierMock.Object, jwtServiceMock.Object);

            var result = await authService.VerifyAndIssueTokensAsync("valid-token");

         
            Assert.Equal("Existing User", result.User.FullName);
            Assert.Single(db.Users);
        }
    }
}
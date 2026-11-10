/*
FirebaseAuth and FirebaseToken
can be used unqualified (without appending the package name)
*/

using FirebaseAdmin.Auth;

namespace FlowFund.Api.Services
{
   /*ITokenVerifier interface is the Firebase verification rulebook.
   Whole app needs it, so Firebase can be mocked or swapped for 
   unit tests.*/
   
    public class FirebaseTokenVerifier : ITokenVerifier
    {
        public async Task<VerifiedTokenInfo> VerifyAsync(string idToken)
        {
            FirebaseToken decoded = await FirebaseAuth.DefaultInstance.VerifyIdTokenAsync(idToken);

            
            decoded.Claims.TryGetValue("email", out var emailClaim);
            decoded.Claims.TryGetValue("name", out var nameClaim);

            return new VerifiedTokenInfo
            {
                Uid = decoded.Uid,
                Email = emailClaim?.ToString(),
                Name = nameClaim?.ToString()
            };
        }
    }
}
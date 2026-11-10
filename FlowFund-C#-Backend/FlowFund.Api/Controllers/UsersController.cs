using FlowFund.Api.Dtos;
using FlowFund.Api.Services;
using Microsoft.AspNetCore.Mvc;

namespace FlowFund.Api.Controllers
{
    //"Me" endpoints: profile, settings, cross-group history, notifications and statements.
    [Route("api/v1/users/me")]
    public class UsersController : ApiControllerBase
    {
        private readonly IUserService _users;
        private readonly IStatementService _statements;

        public UsersController(IUserService users, IStatementService statements)
        {
            _users = users;
            _statements = statements;
        }

        [HttpGet]
        public async Task<ActionResult<UserDto>> GetMe() => Ok(await _users.GetMeAsync(CurrentUserId));

        [HttpPatch]
        public async Task<ActionResult<UserDto>> UpdateProfile([FromBody] UpdateProfileDto dto) =>
            Ok(await _users.UpdateProfileAsync(CurrentUserId, dto));

        [HttpPatch("settings")]
        public async Task<ActionResult<UserDto>> UpdateSettings([FromBody] UpdateSettingsDto dto) =>
            Ok(await _users.UpdateSettingsAsync(CurrentUserId, dto));

        // GET users/me/contributions is served by ContributionsController (same route, one owner).

        [HttpGet("notifications")]
        public async Task<ActionResult<List<NotificationDto>>> Notifications([FromQuery] bool unreadOnly = false) =>
            Ok(await _users.GetNotificationsAsync(CurrentUserId, unreadOnly));

        [HttpPatch("notifications/{id:int}/read")]
        public async Task<ActionResult<NotificationDto>> MarkRead(int id) =>
            Ok(await _users.MarkNotificationReadAsync(CurrentUserId, id));

        //GET statements[?groupId=&amp;from=&amp;to=] - totals computed from stored rows (REQ-STMT-02).
        [HttpGet("statements")]
        public async Task<ActionResult<StatementDto>> Statements(
            [FromQuery] int? groupId,
            [FromQuery(Name = "from")] DateTime? fromDate,
            [FromQuery(Name = "to")] DateTime? toDate) =>
            Ok(await _statements.GetMyStatementAsync(CurrentUserId, groupId, fromDate, toDate, DateTime.UtcNow));
    }
}

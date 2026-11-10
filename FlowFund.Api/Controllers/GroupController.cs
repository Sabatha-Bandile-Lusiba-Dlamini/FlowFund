using System.Security.Claims;
using FlowFund.Api.Common;
using FlowFund.Api.Dtos;
using FlowFund.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;

namespace FlowFund.Api.Controllers
{
    // [Authorize] at the class level — every action here requires a valid JWT
    // (the one AuthController just started issuing). No anonymous access to
    // any group/membership endpoint.
    [ApiController]
    [Route("api/v1/groups")]
    [Authorize]
    public class GroupsController : ControllerBase
    {
        private readonly IGroupService _groupService;

        public GroupsController(IGroupService groupService)
        {
            _groupService = groupService;
        }

        [HttpPost]
        public async Task<ActionResult<GroupDto>> CreateGroup([FromBody] CreateGroupDto dto)
        {
            var result = await _groupService.CreateGroupAsync(CurrentUserId, dto);
            return CreatedAtAction(nameof(GetMyGroups), new { }, result);
        }

        [HttpGet("mine")]
        public async Task<ActionResult<List<GroupDto>>> GetMyGroups()
        {
            var result = await _groupService.GetMyGroupsAsync(CurrentUserId);
            return Ok(result);
        }

        // Each catch block here maps one of GroupService's custom exceptions to
        // the HTTP status code that actually matches its meaning — 403 for
        // "not allowed", 409 for "conflicts with existing state", 404 for
        // "doesn't exist". This is the payoff of defining those exception types
        // instead of just throwing generic Exceptions.
        [HttpPost("{groupId}/members")]
        public async Task<ActionResult<MembershipDto>> AddMember(int groupId, [FromBody] AddMemberDto dto)
        {
            try
            {
                var result = await _groupService.AddMemberAsync(CurrentUserId, groupId, dto);
                return Ok(result);
            }
            catch (ForbiddenException ex) { return StatusCode(StatusCodes.Status403Forbidden, ex.Message); }
            catch (ConflictException ex) { return Conflict(ex.Message); }
        }

        [HttpDelete("{groupId}/members/{userId}")]
        public async Task<IActionResult> RemoveMember(int groupId, int userId)
        {
            try
            {
                await _groupService.RemoveMemberAsync(CurrentUserId, groupId, userId);
                return NoContent();
            }
            catch (NotFoundException ex) { return NotFound(ex.Message); }
            catch (ForbiddenException ex) { return StatusCode(StatusCodes.Status403Forbidden, ex.Message); }
            catch (ConflictException ex) { return Conflict(ex.Message); }
        }

        [HttpPatch("{groupId}/members/{userId}/role")]
        public async Task<ActionResult<MembershipDto>> UpdateMemberRole(int groupId, int userId, [FromBody] UpdateMemberRoleDto dto)
        {
            try
            {
                var result = await _groupService.UpdateMemberRoleAsync(CurrentUserId, groupId, userId, dto);
                return Ok(result);
            }
            catch (NotFoundException ex) { return NotFound(ex.Message); }
            catch (ForbiddenException ex) { return StatusCode(StatusCodes.Status403Forbidden, ex.Message); }
        }

        // Reads the "sub" claim JwtService embedded in the token — this is how
        // the controller knows *which* user is making the request, without
        // ever trusting a userId the client could pass in the request body instead.
        private int CurrentUserId =>
            int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)
                ?? User.FindFirstValue("sub")
                ?? throw new UnauthorizedAccessException());
    }
}

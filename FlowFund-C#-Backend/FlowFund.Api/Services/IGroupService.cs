using FlowFund.Api.Dtos;

namespace FlowFund.Api.Services
{
    public interface IGroupService
    {
        Task<GroupDto> CreateGroupAsync(int creatorUserId, CreateGroupDto dto);
        Task<List<GroupDto>> GetMyGroupsAsync(int userId);
        Task<MembershipDto> AddMemberAsync(int requestingUserId, int groupId, AddMemberDto dto);
        Task RemoveMemberAsync(int requestingUserId, int groupId, int targetUserId);
        Task<MembershipDto> UpdateMemberRoleAsync(int requestingUserId, int groupId, int targetUserId, UpdateMemberRoleDto dto);
    }
}
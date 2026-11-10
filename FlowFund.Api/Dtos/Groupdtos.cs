using FlowFund.Api.Enums;
using FlowFund.Api.Models;

namespace FlowFund.Api.Dtos
{
    public class CreateGroupDto
    {
        public string GroupName { get; set; } = string.Empty;
        public decimal Contribution { get; set; }
        public DateTime CycleStartDate { get; set; }
    }

    public class GroupDto
    {
        public int Id { get; set; }
        public string GroupName { get; set; } = string.Empty;
        public decimal Contribution { get; set; }
        public GroupStatus Status { get; set; }
        public DateTime CycleStartDate { get; set; }
    }

    public class AddMemberDto
    {
        public int UserId { get; set; }
    }

    public class UpdateMemberRoleDto
    {
        public Role Role { get; set; }
    }

    public class MembershipDto
    {
        public int Id { get; set; }
        public int UserId { get; set; }
        public Role Role { get; set; }
        public MembershipStatus Status { get; set; }
    }
}
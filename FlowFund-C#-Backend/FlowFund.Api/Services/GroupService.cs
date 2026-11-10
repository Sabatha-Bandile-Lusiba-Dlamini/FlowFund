using FlowFund.Api.Common;
using FlowFund.Api.Data;
using FlowFund.Api.Dtos;
using FlowFund.Api.Models;
using FlowFund.Api.Enums;
using Microsoft.EntityFrameworkCore;

namespace FlowFund.Api.Services
{
    public class GroupService : IGroupService
    {
        private readonly FlowFundDbContext _db;

        public GroupService(FlowFundDbContext db)
        {
            _db = db;
        }

       
        public async Task<GroupDto> CreateGroupAsync(int creatorUserId, CreateGroupDto dto)
        {
            var group = new StokvelGroup
            {
                StokvelGroupName = dto.GroupName,
                Contribution = dto.Contribution,
                CycleStartDate = dto.CycleStartDate,
                Status = GroupStatus.Forming
            };

            _db.StokvelGroups.Add(group);
            await _db.SaveChangesAsync();

            _db.Memberships.Add(new Membership
            {
                GroupId = group.Id,
                UserId = creatorUserId,
                Role = Role.Admin, 
                Status = MembershipStatus.Active
            });
            await _db.SaveChangesAsync();

            return ToDto(group);
        }

        public async Task<List<GroupDto>> GetMyGroupsAsync(int userId)
        {
            var groupIds = await _db.Memberships
                .Where(m => m.UserId == userId && m.Status == MembershipStatus.Active)
                .Select(m => m.GroupId)
                .ToListAsync();

            var groups = await _db.StokvelGroups
                .Where(g => groupIds.Contains(g.Id))
                .ToListAsync();

            return groups.Select(ToDto).ToList();
        }

        public async Task<MembershipDto> AddMemberAsync(int requestingUserId, int groupId, AddMemberDto dto)
        {
            await EnsureIsAdminAsync(groupId, requestingUserId);

            var existing = await _db.Memberships
                .FirstOrDefaultAsync(m => m.GroupId == groupId && m.UserId == dto.UserId);

            if (existing is not null)
            {
                throw new ConflictException("User is already a member of this group.");
            }

            var membership = new Membership
            {
                GroupId = groupId,
                UserId = dto.UserId,
                Role = Role.Member,
                Status = MembershipStatus.Invited
            };

            _db.Memberships.Add(membership);
            await _db.SaveChangesAsync();

            return ToDto(membership);
        }

       
        public async Task RemoveMemberAsync(int requestingUserId, int groupId, int targetUserId)
        {
            var target = await _db.Memberships
                .FirstOrDefaultAsync(m => m.GroupId == groupId && m.UserId == targetUserId)
                ?? throw new NotFoundException("Membership not found.");

            var isSelfLeave = requestingUserId == targetUserId;

            if (!isSelfLeave)
            {
                await EnsureIsAdminAsync(groupId, requestingUserId);
            }

            if (target.Role == Role.Admin)
            {
                var otherActiveAdmins = await _db.Memberships.CountAsync(m =>
                    m.GroupId == groupId &&
                    m.Role == Role.Admin &&
                    m.Status == MembershipStatus.Active &&
                    m.UserId != targetUserId);

                if (otherActiveAdmins == 0)
                {
                    throw new ConflictException("Cannot remove the last remaining admin of the group.");
                }
            }

            target.Status = MembershipStatus.Removed;
            await _db.SaveChangesAsync();
        }

        public async Task<MembershipDto> UpdateMemberRoleAsync(int requestingUserId, int groupId, int targetUserId, UpdateMemberRoleDto dto)
        {
            await EnsureIsAdminAsync(groupId, requestingUserId);

            var target = await _db.Memberships
                .FirstOrDefaultAsync(m => m.GroupId == groupId && m.UserId == targetUserId)
                ?? throw new NotFoundException("Membership not found.");

            target.Role = dto.Role;
            await _db.SaveChangesAsync();

            return ToDto(target);
        }

       
        private async Task EnsureIsAdminAsync(int groupId, int userId)
        {
            var requesterMembership = await _db.Memberships.FirstOrDefaultAsync(m =>
                m.GroupId == groupId && m.UserId == userId && m.Status == MembershipStatus.Active);

            if (requesterMembership is null || requesterMembership.Role != Role.Admin)
            {
                throw new ForbiddenException("Only a group admin can perform this action.");
            }
        }

        private static GroupDto ToDto(StokvelGroup group) => new()
        {
            Id = group.Id,
            GroupName = group.StokvelGroupName,
            Contribution = group.Contribution,
            Status = group.Status,
            CycleStartDate = group.CycleStartDate
        };

        private static MembershipDto ToDto(Membership membership) => new()
        {
            Id = membership.Id,
            UserId = membership.UserId,
            Role = membership.Role,
            Status = membership.Status
        };
    }
}
using FlowFund.Api.Common;
using FlowFund.Api.Data;
using FlowFund.Api.Dtos;
using FlowFund.Api.Enums;
using FlowFund.Api.Models;
using FlowFund.Api.Services;
using Microsoft.EntityFrameworkCore;
using Xunit;

namespace FlowFund.Api.Tests
{
    public class GroupServiceTests
    {
        private static FlowFundDbContext CreateInMemoryDb(string name)
        {
            var options = new DbContextOptionsBuilder<FlowFundDbContext>()
                .UseInMemoryDatabase(name)
                .Options;
            return new FlowFundDbContext(options);
        }

        [Fact]
        public async Task CreateGroup_MakesCreatorAnAdminMember()
        {
            using var db = CreateInMemoryDb(nameof(CreateGroup_MakesCreatorAnAdminMember));
            var service = new GroupService(db);

            var dto = new CreateGroupDto { GroupName = "Family Stokvel", Contribution = 500m, CycleStartDate = DateTime.UtcNow };
            var group = await service.CreateGroupAsync(creatorUserId: 1, dto);

            var membership = await db.Memberships.FirstAsync(m => m.GroupId == group.Id);
            Assert.Equal(1, membership.UserId);
            Assert.Equal(Role.Admin, membership.Role);
            Assert.Equal(MembershipStatus.Active, membership.Status);
        }

      
        [Fact]
        public async Task AddMember_ThrowsForbidden_WhenRequesterIsNotAdmin()
        {
            using var db = CreateInMemoryDb(nameof(AddMember_ThrowsForbidden_WhenRequesterIsNotAdmin));
            var service = new GroupService(db);

            db.StokvelGroups.Add(new StokvelGroup { Id = 1, StokvelGroupName = "G1", Status = GroupStatus.Active });
            db.Memberships.Add(new Membership { GroupId = 1, UserId = 2, Role = Role.Member, Status = MembershipStatus.Active });
            await db.SaveChangesAsync();

            await Assert.ThrowsAsync<ForbiddenException>(() =>
                service.AddMemberAsync(requestingUserId: 2, groupId: 1, new AddMemberDto { UserId = 3 }));
        }

        [Fact]
        public async Task RemoveMember_AllowsSelfLeave_WithoutAdminCheck()
        {
            using var db = CreateInMemoryDb(nameof(RemoveMember_AllowsSelfLeave_WithoutAdminCheck));
            var service = new GroupService(db);

            db.StokvelGroups.Add(new StokvelGroup { Id = 1, StokvelGroupName = "G1", Status = GroupStatus.Active });
            db.Memberships.Add(new Membership { GroupId = 1, UserId = 5, Role = Role.Admin, Status = MembershipStatus.Active });
            db.Memberships.Add(new Membership { GroupId = 1, UserId = 6, Role = Role.Member, Status = MembershipStatus.Active });
            await db.SaveChangesAsync();

            await service.RemoveMemberAsync(requestingUserId: 6, groupId: 1, targetUserId: 6);

            var membership = await db.Memberships.FirstAsync(m => m.UserId == 6 && m.GroupId == 1);
            Assert.Equal(MembershipStatus.Removed, membership.Status);
        }

        [Fact]
        public async Task RemoveMember_ThrowsConflict_WhenRemovingLastAdmin()
        {
            using var db = CreateInMemoryDb(nameof(RemoveMember_ThrowsConflict_WhenRemovingLastAdmin));
            var service = new GroupService(db);

            db.StokvelGroups.Add(new StokvelGroup { Id = 1, StokvelGroupName = "G1", Status = GroupStatus.Active });
            db.Memberships.Add(new Membership { GroupId = 1, UserId = 7, Role = Role.Admin, Status = MembershipStatus.Active });
            await db.SaveChangesAsync();

            await Assert.ThrowsAsync<ConflictException>(() =>
                service.RemoveMemberAsync(requestingUserId: 7, groupId: 1, targetUserId: 7));
        }
    }
}
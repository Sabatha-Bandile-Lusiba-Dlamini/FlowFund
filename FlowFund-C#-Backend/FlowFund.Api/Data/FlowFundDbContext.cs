using FlowFund.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace FlowFund.Api.Data
{
    public class FlowFundDbContext : DbContext
    {
        public FlowFundDbContext(DbContextOptions<FlowFundDbContext> options) : base(options) { }

        public DbSet<User> Users { get; set; }
        public DbSet<Contribution> Contributions { get; set; }
        public DbSet<Device> Devices { get; set; }
        public DbSet<Meeting> Meetings { get; set; }
        public DbSet<Membership> Memberships { get; set; }
        public DbSet<Notification> Notifications { get; set; }
        public DbSet<PayoutSchedule> PayoutSchedules { get; set; }
        public DbSet<StokvelGroup> StokvelGroups { get; set; }
        public DbSet<Translation> Translations { get; set; }
        public DbSet<Trade> Trades { get; set; }
        public DbSet<JournalEntry> JournalEntries { get; set; }

        // Added — enforces at the database level what AuthService assumes in code:
        // one User row per Firebase identity, and one Membership row per (user, group) pair.
        // Without these, a race condition (e.g. a double-tapped login) could create duplicates
        // that no amount of application-layer checking fully prevents.
        protected override void OnModelCreating(ModelBuilder modelBuilder)
        {
            base.OnModelCreating(modelBuilder);

            modelBuilder.Entity<User>()
                .HasIndex(u => u.FirebaseUid)
                .IsUnique();

            modelBuilder.Entity<Membership>()
                .HasIndex(m => new { m.GroupId, m.UserId })
                .IsUnique();

            // REQ-OFF-03: the client-generated GUID is the idempotency key, so the DATABASE must refuse
            // a second row with the same value - application checks alone lose races.
            modelBuilder.Entity<Contribution>()
                .HasIndex(c => c.ClientGeneratedId)
                .IsUnique();

            // REQ-API-02: money is decimal(18,2). Without this EF picks a default and warns.
            modelBuilder.Entity<Contribution>().Property(c => c.Amount).HasPrecision(18, 2);
            modelBuilder.Entity<StokvelGroup>().Property(g => g.Contribution).HasPrecision(18, 2);

            // Trades and journal entries are private to one user. The unique (UserId, ClientGeneratedId) index is the
            // idempotency guarantee for offline sync retries; the DATABASE enforces it, not just application code.
            modelBuilder.Entity<Trade>().HasIndex(t => new { t.UserId, t.ClientGeneratedId }).IsUnique();
            modelBuilder.Entity<Trade>().HasIndex(t => new { t.UserId, t.TradeDate });
            modelBuilder.Entity<Trade>().Property(t => t.EntryPrice).HasPrecision(18, 6);
            modelBuilder.Entity<Trade>().Property(t => t.ExitPrice).HasPrecision(18, 6);
            modelBuilder.Entity<Trade>().Property(t => t.Quantity).HasPrecision(18, 6);
            modelBuilder.Entity<Trade>().Property(t => t.TradeType).HasMaxLength(4);
            modelBuilder.Entity<JournalEntry>().HasIndex(e => new { e.UserId, e.ClientGeneratedId }).IsUnique();
        }
    }
}
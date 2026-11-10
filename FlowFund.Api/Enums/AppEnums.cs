namespace FlowFund.Api.Enums
{
    // One shared language enum used by both User.LanguagePreference and Translation.LanguageCode,
    // instead of the two separate (and inconsistent) enums in the original files.
    public enum LanguageCode : byte { En, Af, Zu }

    // Kept exactly as in the original Membership.cs — no Owner value exists, so "Admin"
    // is treated as the owner-equivalent role throughout GroupService.
    public enum Role : byte { Member, Treasurer, Admin }

    public enum GroupStatus : byte { Forming, Active, Completed, Cancelled }

    public enum MembershipStatus : byte { Invited, Active, Removed }

    // Renamed from the original shared "Status" enum, scoped to payment-shaped concepts only
    // (Contribution, PayoutSchedule) rather than reused for Group/Membership too.
    // Pending is appended LAST on purpose: Paid=0 and Overdue=1 are already the stored
    // tiny int values, so adding at the end changes no existing row. Because 0 means Paid,
    // every code path that creates a Contribution must set Status explicitly.
    public enum PaymentStatus : byte { Paid, Overdue, Pending }

   
    public enum NotificationType : byte { EarlyReminder, UrgentReminder, Overdue, Confirmation }

    // Pulled out of Device.cs (was a nested, unused enum) and given a property to actually use it.
    public enum Platform { Android, IOS }

    public enum PayoutStatus : byte
    {
        Scheduled,
        Completed,
        Skipped
    }

    public enum RotationStrategy : byte
    {
        Random,
        JoinDate,
        Manual
    }

    


}

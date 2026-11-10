namespace FlowFund.Api.Services
{
    /// <summary>Converts between the app's epoch-millisecond timestamps and UTC DateTime values.</summary>
    public static class EpochTime
    {
        public static bool TryFromMillis(long millis, out DateTime utc)
        {
            try
            {
                utc = DateTimeOffset.FromUnixTimeMilliseconds(millis).UtcDateTime;
                return true;
            }
            catch (ArgumentOutOfRangeException)
            {
                utc = default;
                return false;
            }
        }

        // EF Core returns datetime2 values as Kind=Unspecified; they are stored as UTC, so say so explicitly.
        public static long ToMillis(DateTime utc) =>
            new DateTimeOffset(DateTime.SpecifyKind(utc, DateTimeKind.Utc)).ToUnixTimeMilliseconds();
    }
}

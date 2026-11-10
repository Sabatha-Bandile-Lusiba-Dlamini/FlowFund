using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Design;

namespace FlowFund.Api.Data
{
 
    public class FlowFundDbContextFactory : IDesignTimeDbContextFactory<FlowFundDbContext>
    {
        
        public FlowFundDbContext CreateDbContext(string[] args)
        {
           
            var connectionString = Environment.GetEnvironmentVariable("FLOWFUND_CONNECTION_STRING")
                            ?? throw new InvalidOperationException(
                                "Set the FLOWFUND_CONNECTION_STRING environment variable before running dotnet ef.");

            var optionsBuilder = new DbContextOptionsBuilder<FlowFundDbContext>();
            optionsBuilder.UseSqlServer(connectionString);

            return new FlowFundDbContext(optionsBuilder.Options);
        }
    }
}


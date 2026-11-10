using FirebaseAdmin;
using FlowFund.Api.Data;
using FlowFund.Api.Models;
using FlowFund.Api.Middleware;
using FlowFund.Api.Services;
using Google.Apis.Auth.OAuth2;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi;


var builder = WebApplication.CreateBuilder(args);

// Controllers 
// One AddControllers call only (your file had two). Enums travel as strings ("Pending", "Paid")
// instead of numbers, which is what the Android client expects.
builder.Services.AddControllers()
    .AddJsonOptions(options =>
        options.JsonSerializerOptions.Converters.Add(new System.Text.Json.Serialization.JsonStringEnumConverter()));

builder.Services.AddHttpContextAccessor();
builder.Services.AddScoped<FlowFund.Api.Services.CurrentUserService>();
builder.Services.AddScoped<FlowFund.Api.Services.PayoutRotationCalculatorService>();



// EF Core + Azure SQL 
// EnableRetryOnFailure rides out the serverless database waking from auto-pause.
builder.Services.AddDbContext<FlowFundDbContext>(options =>
    options.UseSqlServer(
        builder.Configuration.GetConnectionString("DefaultConnection"),
        sqlOptions => sqlOptions.EnableRetryOnFailure(
            maxRetryCount: 5,
            maxRetryDelay: TimeSpan.FromSeconds(10),
            errorNumbersToAdd: null)));

// Firebase Admin SDK 
// Hosted: the key JSON comes from the Firebase__ServiceAccountJson app setting (no file on the server).
// Local: it still loads from the file named in Firebase:ServiceAccountPath.
string? firebaseJson = builder.Configuration["Firebase:ServiceAccountJson"];
string? firebasePath = builder.Configuration["Firebase:ServiceAccountPath"];

GoogleCredential googleCredential;
if (!string.IsNullOrWhiteSpace(firebaseJson))
{
    googleCredential = CredentialFactory.FromJson<ServiceAccountCredential>(firebaseJson).ToGoogleCredential();
}
else if (!string.IsNullOrWhiteSpace(firebasePath))
{
    googleCredential = CredentialFactory.FromFile<ServiceAccountCredential>(firebasePath).ToGoogleCredential();
}
else
{
    throw new InvalidOperationException("Set Firebase:ServiceAccountJson or Firebase:ServiceAccountPath.");
}

FirebaseApp.Create(new AppOptions { Credential = googleCredential });

// Services (Scoped: they use the per-request DbContext) 
builder.Services.AddScoped<ITokenVerifier, FirebaseTokenVerifier>();
builder.Services.AddScoped<IJwtService, JwtService>();
builder.Services.AddScoped<IAuthService, AuthService>();
builder.Services.AddScoped<IGroupService, GroupService>();
builder.Services.AddScoped<IUserService, UserService>();
builder.Services.AddScoped<IStatementService, StatementService>();
builder.Services.AddScoped<ITradeService, TradeService>();
builder.Services.AddScoped<IJournalService, JournalService>();

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = builder.Configuration["Jwt:Issuer"],
            ValidAudience = builder.Configuration["Jwt:Audience"],
            IssuerSigningKey = new SymmetricSecurityKey(
                Convert.FromBase64String(builder.Configuration["Jwt:Secret"]!))
        };

        options.Events = new JwtBearerEvents
        {
            OnTokenValidated = ctx =>
            {
                Console.WriteLine("✅ Security pipeline token verification succeeded!");
                return Task.CompletedTask;
            },
            // This will catch the exact error thrown after the 834ms network validation step
            OnAuthenticationFailed = ctx =>
            {
                Console.WriteLine($"\n[JWT DIAGNOSTICS] Validation failed after network call.");
                Console.WriteLine($"Exception Type: {ctx.Exception.GetType().Name}");
                Console.WriteLine($"Error Message: {ctx.Exception.Message}\n");
                return Task.CompletedTask;
            }
        };
    });




builder.Services.AddAuthorization();
builder.Services.AddEndpointsApiExplorer();


builder.Services.AddSwaggerGen(options =>
{
    var scheme = new Microsoft.OpenApi.OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = Microsoft.OpenApi.SecuritySchemeType.ApiKey,
        Scheme = JwtBearerDefaults.AuthenticationScheme,
        BearerFormat = "JWT",
        In = Microsoft.OpenApi.ParameterLocation.Header,
        Description = "Enter your JWT token in the format: Bearer {your_token_here}"
    };

    options.AddSecurityDefinition(JwtBearerDefaults.AuthenticationScheme, scheme);

    // Explicitly forward the doc context parameter into the reference initializer
    options.AddSecurityRequirement((doc) =>
    {
        var securityRequirement = new Microsoft.OpenApi.OpenApiSecurityRequirement();

        // PASS 'doc' HERE so OpenAPI can bind your security definitions internally
        var schemeKey = new Microsoft.OpenApi.OpenApiSecuritySchemeReference(JwtBearerDefaults.AuthenticationScheme, doc);

        securityRequirement.Add(schemeKey, new List<string>());

        return securityRequirement;
    });
});







var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseHttpsRedirection();

// New: first after HTTPS so it wraps everything below. Logs every request and turns the
// service-layer exceptions (403, 404, 409, ...) into proper HTTP responses.
app.UseMiddleware<ApiExceptionMiddleware>();

// Order matters: Authentication before Authorization (your file had UseAuthentication twice).
app.UseAuthentication();
app.UseAuthorization();

app.MapControllers();

app.Run();
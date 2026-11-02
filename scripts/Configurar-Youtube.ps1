param(
    [switch]$Persistir
)

$ErrorActionPreference = 'Stop'
$segredo = Read-Host 'Cole sua chave da YouTube Data API v3 (entrada oculta)' -AsSecureString
$chave = $null

try {
    $chave = [System.Net.NetworkCredential]::new('', $segredo).Password.Trim()
    if ([string]::IsNullOrWhiteSpace($chave)) {
        throw 'Nenhuma chave informada. A configuracao nao foi alterada.'
    }

    # Consulta real: a chave fica no cabecalho, nunca na URL ou na saida.
    $url = 'https://www.googleapis.com/youtube/v3/videos?part=status&id=M7lc1UVf-VE'
    try {
        $resposta = Invoke-RestMethod -Method Get -Uri $url -TimeoutSec 10 -Headers @{
            'X-Goog-Api-Key' = $chave
        }
    } catch {
        throw 'A consulta ao YouTube falhou. Confira ativacao da API, chave, restricoes, quota e conexao. A configuracao nao foi alterada.'
    }

    if ($resposta.items.Count -eq 0) {
        throw 'A API respondeu, mas o video de exemplo nao esta disponivel. A configuracao nao foi alterada.'
    }

    if ($Persistir) {
        [Environment]::SetEnvironmentVariable('YOUTUBE_API_KEY', $chave, 'User')
        Write-Host 'Chave salva nas variaveis do seu usuario Windows.'
    }
    $env:YOUTUBE_API_KEY = $chave
    Write-Host 'YOUTUBE_API_KEY configurada neste terminal. A consulta real ao YouTube funcionou.'
    Write-Host 'Inicie o backend por este terminal. Para executar pelo VS Code, feche e reabra o aplicativo apos persistir a chave.'
} finally {
    $chave = $null
    $segredo.Dispose()
}

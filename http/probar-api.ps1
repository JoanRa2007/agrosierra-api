# Pruebas rapidas de AgroAPI - Autor: Joan Rodriguez
# Requiere el backend corriendo en http://localhost:8080
$base = "http://localhost:8080/api/v1"

function Llamar($titulo, $esperado, $metodo, $ruta, $token, $cuerpo) {
    $h = @{}
    if ($token) { $h["Authorization"] = "Bearer $token" }
    $p = @{ Uri = "$base$ruta"; Method = $metodo; Headers = $h; ContentType = "application/json"; UseBasicParsing = $true }
    if ($cuerpo) { $p.Body = [System.Text.Encoding]::UTF8.GetBytes($cuerpo) }
    try {
        $r = Invoke-WebRequest @p
        $codigo = [int]$r.StatusCode
        $texto = $r.Content
    } catch {
        if ($null -eq $_.Exception.Response) {
            Write-Host "ERROR: el backend no responde. Levantalo primero en IntelliJ (Aplicacion.kt)." -ForegroundColor Red
            exit 1
        }
        $codigo = [int]$_.Exception.Response.StatusCode
        $texto = $_.ErrorDetails.Message
    }
    $color = if ($codigo -eq $esperado) { "Green" } else { "Red" }
    Write-Host ""
    Write-Host "== $titulo" -ForegroundColor Cyan
    Write-Host "   $metodo $ruta  ->  $codigo (esperado $esperado)" -ForegroundColor $color
    if ($texto) {
        $corto = if ($texto.Length -gt 300) { $texto.Substring(0, 300) + "..." } else { $texto }
        Write-Host "   $corto"
    }
    return $texto
}

function Token($usuario) {
    $t = Llamar "Login $usuario" 200 "POST" "/auth/login" $null "{`"usuario`":`"$usuario`",`"contrasena`":`"Agro2026*`"}"
    return ($t | ConvertFrom-Json).token
}

Write-Host "PRUEBAS DE AGROAPI - Joan Rodriguez" -ForegroundColor Yellow
$tec = Token "tecnico1"
$jefe = Token "jefe1"
$adm = Token "admin"

$labor = '{"loteId":1,"tipo":"RIEGO","fecha":"2026-10-27","responsable":"Joan Rodriguez","cantidad":12.5,"observacion":"Prueba"}'

Llamar "Login con contrasena incorrecta" 401 "POST" "/auth/login" $null '{"usuario":"jefe1","contrasena":"mala"}' | Out-Null
Llamar "Usuario actual" 200 "GET" "/usuarios/me" $tec | Out-Null
Llamar "Padron paginado" 200 "GET" "/lotes?pagina=0&tamanio=3" $tec | Out-Null
Llamar "Lote inexistente" 404 "GET" "/lotes/999" $tec | Out-Null
$creada = Llamar "Registrar labor" 201 "POST" "/labores" $tec $labor
$id = ($creada | ConvertFrom-Json).id
Llamar "Corregir labor propia" 200 "PUT" "/labores/$id" $tec $labor | Out-Null
Llamar "Eliminar labor como jefe" 204 "DELETE" "/labores/$id" $jefe | Out-Null
Llamar "401 Sin token" 401 "GET" "/lotes" $null | Out-Null
# Se altera el CONTENIDO del token (payload) sin volver a firmarlo
$partes = $tec.Split('.')
$partes[1] = $partes[1].Substring(0, $partes[1].Length - 6) + "AAAAAA"
Llamar "401 Token alterado" 401 "GET" "/lotes" ($partes -join '.') | Out-Null
Llamar "403 Tecnico intenta eliminar" 403 "DELETE" "/labores/1" $tec | Out-Null
Llamar "403 Tecnico intenta cerrar campania" 403 "POST" "/lotes/1/cerrar-campania" $tec | Out-Null
Llamar "403 Admin intenta registrar labor" 403 "POST" "/labores" $adm $labor | Out-Null
Llamar "400 Validacion por campo" 400 "POST" "/labores" $tec '{"loteId":1,"tipo":"RIEGO","fecha":"2026-10-27","responsable":"","cantidad":-5}' | Out-Null
Llamar "400 RN-04 Dosis excesiva" 400 "POST" "/labores" $tec '{"loteId":1,"tipo":"FUMIGACION","fecha":"2026-10-27","responsable":"Joan Rodriguez","cantidad":50,"observacion":""}' | Out-Null
Llamar "409 RN-01 Lote con campania cerrada" 409 "POST" "/labores" $tec '{"loteId":7,"tipo":"RIEGO","fecha":"2026-10-27","responsable":"Joan Rodriguez","cantidad":10,"observacion":""}' | Out-Null
Llamar "409 RN-03 Cerrar lote sin cosecha" 409 "POST" "/lotes/2/cerrar-campania" $jefe | Out-Null

Write-Host ""
Write-Host "Listo. Verde = correcto. Toma captura de esta ventana." -ForegroundColor Yellow

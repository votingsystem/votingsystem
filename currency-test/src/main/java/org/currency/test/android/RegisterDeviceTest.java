package org.currency.test.android;

import org.currency.test.Constants;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.OperationCheckerDto;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.currency.RegisterDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyOperation;

import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RegisterDeviceTest extends BaseTest {

    private static final Logger log = Logger.getLogger(RegisterDeviceTest.class.getName());

    public static final String REQUEST = "-----BEGIN PKCS7-----\n" +
            "MIAGCSqGSIb3DQEHAqCAMIACAQExDzANBglghkgBZQMEAgEFADCABgkqhkiG9w0B\n" +
            "BwGggCSABIID6HsiY3NyIjoiLS0tLS1CRUdJTiBDRVJUSUZJQ0FURSBSRVFVRVNU\n" +
            "LS0tLS1cbk1JSURSakNDQWk0Q0FRQXdPekVQTUEwR0ExVUVCQk1HUjBGU1EwbEJN\n" +
            "UlF3RWdZRFZRUXFFd3RLVDFORklFcEJcblZrbEZVakVTTUJBR0ExVUVCUk1KTURj\n" +
            "MU5UTXhOekpJTUlJQklqQU5CZ2txaGtpRzl3MEJBUUVGQUFPQ0FROEFcbk1JSUJD\n" +
            "Z0tDQVFFQXU0NDVZTkRuU3lyWlFyWUFSYlJqeTN6bG5wSXUzUUc5d1BwQjFDam1L\n" +
            "Vk1wZGgrdTNISzhcbktOdkhPRlNIMEdCcURhdDdNc0x3dVlWNWtGdk54eUMydUU4\n" +
            "bVpxVHNJWC9hWW5NNVF4UVVUa3BjN3d3QUhXazdcbjdnd1JTa2FBc2J1dzM0WVN3\n" +
            "Mm92TVdoVy8yeDczdWJ5bStQL0tYMExrNmIrUk5ET1V4Q2tGZFdPS05hSlFwK0lc\n" +
            "bm11N0ZYaU1ZWXB5NHBxUjNiUzBhUmQ1RFZYZWpaK2RpZzErN3gwTDdWQUNiUGpT\n" +
            "RWVySmtTcTR0bFlaWU8zSExcblZXNC9xTFdhODBhYlE0M0FKRGZ1NXIzN0diVDhr\n" +
            "U2ltUGlTaHhJNms2RWNFQVRuRWpBdENNYm5OREFlTTRjTCtcbmtPN1FselZPNDJY\n" +
            "b2w3T2s3Q3F5MGFBQ05oT1VzVWtzSFFJREFRQUJvSUhGTUlIQ0Jna0FBQUFBQUFB\n" +
            "QUFBUXhcbmdiUU1nYkY3SW1SbGRtbGpaVTVoYldVaU9pSmpkWEp5Wlc1amVTMXRi\n" +
            "MkpwYkdVdFlYQndiR2xqWVhScGIyNGlcbkxDSmtaWFpwWTJWVWVYQmxJam9pVFU5\n" +
            "Q1NVeEZJaXdpWjJsMlpXNXVZVzFsSWpvaVNrOVRSU0JLUVZaSlJWSWlcbkxDSnVk\n" +
            "VzFKWkNJNklqQTNOVFV6TVRjeVNDSXNJbk4xY201aGJXVWlPaUpIUVZKRFNVRWlM\n" +
            "Q0oxZFdsa0lqb2lcbk9HVXlaVFl6WWpZdE9EUXhOaTAwWlRSaExXSTNNVEF0T0Rn\n" +
            "NU9ESmpPVEF4WldZMkluMHdEUVlKS29aSWh2Y05cbkFRRUxCUUFEZ2dFQkFHbi8w\n" +
            "S2RDcFRoN2VLT0YrZzJYZ2N6Z0MwakNCRy9MTzZBakpsNWdyMjkzbEpDeDBLMHJc\n" +
            "bkxCZTlSNndqTXRhZVBIbWMvWGpOcndNMEU1TGdBVllPb2Z3NHZMeENLTDZTMkNv\n" +
            "dXpBTUxnd0pKaXVHaVNZRUFcbk1sdS96NjhXakx4ejZ6eGxYaWh4SThaT0lzem1t\n" +
            "YTQEggJjS1pOM2JoM0Y3Y1Irdk9HT0hIRFhWc2ErUkphK0RoSkR6XG45NE9jRU03\n" +
            "cWpCMTdpV3UzRHl1dFV5TFFiL3NySmFZN214ck9DLzVGVEFKUlZzbi95VStSaWZX\n" +
            "OGhwaG1IY3RnXG5OSTh0eGZXUDB1WDJ3NFBTbTZtayt3cFJBTGJRaGo4SUpBNGdV\n" +
            "WEhtdnVDbE1nS0VoNEF3M3hQYW1TS2Z5cWFwXG5CcllMbW5VMitNMmI2VnlBUVht\n" +
            "VzFVVFVvbytmMzRJSlhhRT1cbi0tLS0tRU5EIENFUlRJRklDQVRFIFJFUVVFU1Qt\n" +
            "LS0tLVxuIiwiZGV2aWNlSWQiOiI4ZTJlNjNiNi04NDE2LTRlNGEtYjcxMC04ODk4\n" +
            "MmM5MDFlZjYiLCJvcGVyYXRpb24iOnsiRW50aXR5SWQiOiJodHRwczovL3ZvdGlu\n" +
            "Zy5kZG5zLm5ldC9pZHByb3ZpZGVyIiwiVHlwZSI6IlJFR0lTVEVSX0RFVklDRSJ9\n" +
            "LCJ1c2VyIjp7ImZ1bGxOYW1lIjoiSk9TRSBKQVZJRVIgR0FSQ0lBIiwiQ291bnRy\n" +
            "eSI6IkdBUkNJQSBaT1JOT1pBLCBKT1NFIEpBVklFUiAoRklSTUEpIiwiQ2VydGlm\n" +
            "aWNhdGVzIjpbeyJyb290IjpmYWxzZX1dLCJHaXZlbk5hbWUiOiJKT1NFIEpBVklF\n" +
            "UiIsIk51bUlkIjoiMDc1NTMxNzJIIiwiU3VyTmFtZSI6IkdBUkNJQSJ9LCJ1dWlk\n" +
            "IjoiNDZjNDhmNzgtMmMzYS00NDYwLWIwODItMTNiM2I1MzdiNzg1In0AAAAAAACg\n" +
            "gDCCBeQwggTMoAMCAQICEFgD3bb+7psjVhIgg2DRiZ8wDQYJKoZIhvcNAQEFBQAw\n" +
            "XDELMAkGA1UEBhMCRVMxKDAmBgNVBAoMH0RJUkVDQ0lPTiBHRU5FUkFMIERFIExB\n" +
            "IFBPTElDSUExDTALBgNVBAsMBEROSUUxFDASBgNVBAMMC0FDIEROSUUgMDAxMB4X\n" +
            "DTE1MTAwNTA3MDIyN1oXDTIwMTAwNTA3MDIyNlowdjELMAkGA1UEBhMCRVMxEjAQ\n" +
            "BgNVBAUTCTA3NTUzMTcySDEPMA0GA1UEBAwGR0FSQ0lBMRQwEgYDVQQqDAtKT1NF\n" +
            "IEpBVklFUjEsMCoGA1UEAwwjR0FSQ0lBIFpPUk5PWkEsIEpPU0UgSkFWSUVSIChG\n" +
            "SVJNQSkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCZhAwv7JuGvcNa\n" +
            "JkScUVDRcZIm6JOndniK0c+i7VLwhvtYP6mroUzP7iStSjl+8ruZ72Lm/UlA5kM/\n" +
            "/QaBFPI68/dEIfoOoPdhZbJ1bRe+RwGMtdN3wsYkLelzqUVDDxgjxvmkk7O/LJOA\n" +
            "34EUTAO2zG5Y+AsMXQk0oo/MeZhP3f7rjuxPMflZGmL8FwMnp1fMpBjEdTidMBgp\n" +
            "ddyfj0jNi5n5JVe5vXigRUJLwi+Aw+8dlN/KuFxJ+MB8t1eQbh96wNYiMz9RMXFw\n" +
            "GzVfk1yvJWb8MsgWhmB9u6EqUfojisldknivNsIDymYxJJuFm++JiBcbo2ItVBdQ\n" +
            "cfddNxN/AgMBAAGjggKGMIICgjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQWBBTFfMGW\n" +
            "lSaDbKLYRFvW6Z50ZbFn/DAfBgNVHSMEGDAWgBQaiajF7o92XVVxifM7Nb2qBQCV\n" +
            "bzAiBggrBgEFBQcBAwQWMBQwCAYGBACORgEBMAgGBgQAjkYBBDBgBggrBgEFBQcB\n" +
            "AQRUMFIwHwYIKwYBBQUHMAGGE2h0dHA6Ly9vY3NwLmRuaWUuZXMwLwYIKwYBBQUH\n" +
            "MAKGI2h0dHA6Ly93d3cuZG5pZS5lcy9jZXJ0cy9BQ1JhaXouY3J0MDsGA1UdIAQ0\n" +
            "MDIwMAYIYIVUAQICAgMwJDAiBggrBgEFBQcCARYWaHR0cDovL3d3dy5kbmllLmVz\n" +
            "L2RwYzCB8AYIKwYBBQUHAQIEgeMwgeAwMgIBATALBglghkgBZQMEAgEEIIxSEvSe\n" +
            "gOIMR+tChg8zwM4zcwVGjXLHdKDCkulLWco5MDICAQAwCwYJYIZIAWUDBAIBBCCr\n" +
            "WwaBM3z3f0INj+t7Co//tSi5EaQoDthDM/LaP1jfLDA6BglghVQBAgIEAgEwCwYJ\n" +
            "YIZIAWUDBAIBBCCM6I8ToupRlRIWRUkELC190RoV4FnzJwmTCMqlBEiruTA6Bglg\n" +
            "hVQBAgIEAgYwCwYJYIZIAWUDBAIBBCDpMpVoo/Ne6Ni+4Lo/YtLTTWs8HJvTq3MA\n" +
            "zE2NSZP/fTAoBgNVHQkEITAfMB0GCCsGAQUFBwkBMREYDzE5NzEwMzE4MTIwMDAw\n" +
            "WjBCBghghVQBAgIEAQQ2MDQwMgIBAjALBglghkgBZQMEAgEEIJj2G/9jKaeQlq1+\n" +
            "KwrAT3ooChToMcDFwJI8tMKbwjwNMA4GA1UdDwEB/wQEAwIGQDANBgkqhkiG9w0B\n" +
            "AQUFAAOCAQEAdRCj8GYqDAlFnvvS+yjcrIqYWUJ3ge/dzNzQOp1fOSq5dup7LE5H\n" +
            "glD6Twok2rMjf8bul5nZWM/QS0cN6mnrN6WrCWBCfiRf8ZYLsawzQosnKDebL4Ff\n" +
            "ElgbFQLeIp03CeCY69ESg9crzvCA2tadJ96jETaklb5iRY/6c8/W6qvR0m0kukdi\n" +
            "xSlgKb/91TkFtvIzXWkkPXPx8wiZLYwI6OxMbqEZhLCuWfpLy2KbO0BbOQ0+ZaDO\n" +
            "e5VGpxRwMJlwQNyvPI8ph48SK4RjvFgjW2aYh/jpfZYSVtovCXRP5jPEpvkrvggr\n" +
            "qcPuINR19wAP1YpZF3pa9YP5lJiaqTTcUjCCBcUwggOtoAMCAQICEGQgZsmZe67h\n" +
            "RALabqQi1kkwDQYJKoZIhvcNAQEFBQAwXTELMAkGA1UEBhMCRVMxKDAmBgNVBAoM\n" +
            "H0RJUkVDQ0lPTiBHRU5FUkFMIERFIExBIFBPTElDSUExDTALBgNVBAsMBEROSUUx\n" +
            "FTATBgNVBAMMDEFDIFJBSVogRE5JRTAeFw0wNjAyMjcxMDU0MzhaFw0yMTAyMjYy\n" +
            "MjU5NTlaMFwxCzAJBgNVBAYTAkVTMSgwJgYDVQQKDB9ESVJFQ0NJT04gR0VORVJB\n" +
            "TCBERSBMQSBQT0xJQ0lBMQ0wCwYDVQQLDARETklFMRQwEgYDVQQDDAtBQyBETklF\n" +
            "IDAwMTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKz+SpnS5Kf9jQ/u\n" +
            "e4y51/uJNX/Omd+kxq78tzLgDVfn7fDWzTsWXKXBkj8OlmGvptMyn4UPrEBEx4TB\n" +
            "zKOxm+p5YagpjU6U3Y9+StAZ+Zmi50xPBfP6f+qU/Qi6xD2FeXl+zmzQoqpNr57l\n" +
            "kDHFQMEV9O+0GBijc1vfKfSsYquJmGUUdDm58WDXetVg3Mznbj+Qv3VCKxpNyXxS\n" +
            "yDSxldHf/RGnh5e845MzmMgknBqvrZa5ClYFugUMCP4F8OF0WDYRYwgD82/HGi3r\n" +
            "8UMRp80VGfHUzlDnqoDmdRV3zbooOyqHpOKpHckCqMGDaeEtzcHrrr27GzyWalcw\n" +
            "qs8AqvcCAwEAAaOCAYAwggF8MBIGA1UdEwEB/wQIMAYBAf8CAQAwHQYDVR0OBBYE\n" +
            "FBqJqMXuj3ZdVXGJ8zs1vaoFAJVvMB8GA1UdIwQYMBaAFI5F9J9zxf8vGwXbAUdg\n" +
            "GwOKgbe6MA4GA1UdDwEB/wQEAwIBBjA3BgNVHSAEMDAuMCwGBFUdIAAwJDAiBggr\n" +
            "BgEFBQcCARYWaHR0cDovL3d3dy5kbmllLmVzL2RwYzCB3AYDVR0fBIHUMIHRMIHO\n" +
            "oIHLoIHIhiBodHRwOi8vY3Jscy5kbmllLmVzL2NybHMvQVJMLmNybIaBo2xkYXA6\n" +
            "Ly9sZGFwLmRuaWUuZXMvQ049Q1JMLENOPUFDJTIwUkFJWiUyMEROSUUsT1U9RE5J\n" +
            "RSxPPURJUkVDQ0lPTiUyMEdFTkVSQUwlMjBERSUyMExBJTIwUE9MSUNJQSxDPUVT\n" +
            "P2F1dGhvcml0eVJldm9jYXRpb25MaXN0P2Jhc2U/b2JqZWN0Y2xhc3M9Y1JMRGlz\n" +
            "dHJpYnV0aW9uUG9pbnQwDQYJKoZIhvcNAQEFBQADggIBAGdY/2uyHzpnCf4HqyLl\n" +
            "GJwRJOh25dOaY0k7Wjn97fAsUufeONInnRg3dm7gGx1aFJSt3eEPJ79KrLNFzJ+g\n" +
            "raO+QhZ9hIcsWXpOng425gVHCu1TxFpIzHUGVIwyMWmuJn28TBgPgcdkn39M43Hb\n" +
            "LsRJw82F5q/ZZT+36l/PPezEWqCj+SpO2GnLuPY67T1GEwrQB7p2Q8rbBkji7fE4\n" +
            "RQ2ovJCLPB3/HYibwVBo8vRZUnqKn4rgL4HTybVZzXncbRHqOf6BnN/vS9NjxqqO\n" +
            "KD4MbRQnCdjpbDL4eY8X/Flv6HJJ1bqTbXmyqtO9kO9qCyAsvb2kQrecRk/pVNKE\n" +
            "WF0/jeNXnh2FuUhzElas23NrieKWMTBZY08eYj9DbCrDsDeoq/zspx7BzWwvYJY9\n" +
            "dD4kkJDHVPKg8oG3/+DhWMM/+EaM4WjFMw9mF7YjqRfFOUcnohBw2dJQpCeqRre3\n" +
            "Biz4S8s6mHfIs2E7VFVzZiRq4+eH9QBuFCosfSMPF89VsgI20ro/GZ/OBT8iK7IC\n" +
            "LZX5LRkoDMRdfOW2s8ULR8qL+XgaLIvR/jwZfgj3kNtzsybQiuBbLHaZT3TAQHh2\n" +
            "hiJ8S2ab7daoyZzSLNEGlysh4+j3KDdTA4GKOclgI9WTxeEocYlOkwRx/UscLbTZ\n" +
            "sZChZDWBcE95yqvY+uikdwtnAAAxggjaMIII1gIBATBwMFwxCzAJBgNVBAYTAkVT\n" +
            "MSgwJgYDVQQKDB9ESVJFQ0NJT04gR0VORVJBTCBERSBMQSBQT0xJQ0lBMQ0wCwYD\n" +
            "VQQLDARETklFMRQwEgYDVQQDDAtBQyBETklFIDAwMQIQWAPdtv7umyNWEiCDYNGJ\n" +
            "nzANBglghkgBZQMEAgEFAKCCBzswGAYJKoZIhvcNAQkDMQsGCSqGSIb3DQEHATAc\n" +
            "BgkqhkiG9w0BCQUxDxcNMTcwNDIwMTcwNTQ1WjAvBgkqhkiG9w0BCQQxIgQgSCTA\n" +
            "0kBjWkaLrv9FHLpaZTFlhOozFn4QIj5jqiEbhAcwggbOBgsqhkiG9w0BCRACDjGC\n" +
            "Br0wgga5BgkqhkiG9w0BBwKgggaqMIIGpgIBAzEPMA0GCWCGSAFlAwQCAQUAMHkG\n" +
            "CyqGSIb3DQEJEAEEoGoEaDBmAgEBBgaCEoQ3hnowMTANBglghkgBZQMEAgEFAAQg\n" +
            "SCTA0kBjWkaLrv9FHLpaZTFlhOozFn4QIj5jqiEbhAcCCDw00IV/Ya7JGA8yMDE3\n" +
            "MDQyMDE1MDUzM1owCwIBAYACAfSBAgH0oIID1jCCA9IwggK6oAMCAQICBgFbGvFa\n" +
            "MjANBgkqhkiG9w0BAQsFADAsMRowGAYDVQQDDBFGQUtFIFJPT1QgRE5JZSBDQTEO\n" +
            "MAwGA1UECwwFQ2VydHMwHhcNMTcwMzI5MTQ0MTE2WhcNMTgwMzI5MTQ0MTE2WjAr\n" +
            "MSkwJwYDVQQqEyBWb3RpbmctQ3VycmVuY3kgVGltZXN0YW1wIFNlcnZlcjCCASIw\n" +
            "DQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIKkn9q4bqQYFm/wM7zTbXaoqbjE\n" +
            "oeMrO7ACgxAJRcTYKjGFOUJfH3joQ/bnxNyAumV+X8KukM9VbDW10dgYJyiKiKKs\n" +
            "h71PLiaU8fv7WyP7uHJ4ScwJ6Lo+mlFkuzYo1ZlmGwUK6RsQ8VdQ2JMMpU3oFrGa\n" +
            "1WA3r4cIpKhdUHU+muaKdLzdZsBbmDeEs/whwMjeyMiJouQGY1LO3TPdHUDNCpkF\n" +
            "/HHpfORSkPeTijsdolpWHz1J+iw1EEW5ROH1h2RcqSmbWpP/ZJpw3e0FMiECAjQD\n" +
            "haXzihevRSZ8sYowRtjUz/+zIQBJvd5PiGMGZjgwlaBfGn3LAD1IvS9CwLcCAwEA\n" +
            "AaOB+jCB9zBDBggrBgEFBQcBAQQ3MDUwMwYIKwYBBQUHMAGGJ2h0dHBzOi8vdm90\n" +
            "aW5nLmRkbnMubmV0L2lkcHJvdmlkZXIvb2NzcDBbBgNVHSMEVDBSgBQ9mNuHs0ML\n" +
            "YMMDMD1iarJr0LxcMKEwpC4wLDEaMBgGA1UEAwwRRkFLRSBST09UIEROSWUgQ0Ex\n" +
            "DjAMBgNVBAsMBUNlcnRzgghvzDD4vCDU6jAdBgNVHQ4EFgQUoW39F3vfAC49S9p6\n" +
            "TXDtUgmArIIwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCBaAwFgYDVR0lAQH/\n" +
            "BAwwCgYIKwYBBQUHAwgwDQYJKoZIhvcNAQELBQADggEBAJ3FI/flbzJ9pbWbnCLw\n" +
            "y7JiCG0POrfqZo/s4jVkj0WjEEEGf02uxeGmJtj7SDiOYPFIUvn0953NeKV5O6Ns\n" +
            "KA50JBv+nytiMBUNyn6Ctmg1Os6IRxsewT2D7rM2I/Cv6R4484OTLNe2Hy+OQyLv\n" +
            "7Q4I+jtGg/bYxbAH7C9d2hWHwcgzS3mtz4MyvZwTMRLTDCMZwpoIkpDRnfinCcX0\n" +
            "asDPSRRfsVFW2g/u7dZXQkJmD9qcraxH7WtFrRlD4RVFnlr9lhid65bWGUjYtR6v\n" +
            "5OsSYuxj1iMaoVeSgDDgY4snbQacquv8nWSW+ADSIbhI0bMAMm1ZY5a0HDAUJoxc\n" +
            "mUYxggI5MIICNQIBATA2MCwxGjAYBgNVBAMMEUZBS0UgUk9PVCBETkllIENBMQ4w\n" +
            "DAYDVQQLDAVDZXJ0cwIGAVsa8VoyMA0GCWCGSAFlAwQCAQUAoIHTMBoGCSqGSIb3\n" +
            "DQEJAzENBgsqhkiG9w0BCRABBDAcBgkqhkiG9w0BCQUxDxcNMTcwNDIwMTUwNTMz\n" +
            "WjAtBgkqhkiG9w0BCTQxIDAeMA0GCWCGSAFlAwQCAQUAoQ0GCSqGSIb3DQEBAQUA\n" +
            "MC8GCSqGSIb3DQEJBDEiBCAd3R5poeqnOBz36sJfkSCA19vNu2vx68OCjRzQWSW9\n" +
            "RDA3BgsqhkiG9w0BCRACLzEoMCYwJDAiBCA6aOW3NOxOOBtTCU48/YU9gfiwcufw\n" +
            "EzKMHW3Cb1JQFDANBgkqhkiG9w0BAQEFAASCAQBveNNI049Cub+m8huba4Iwsu0G\n" +
            "oaMd7JVeF/9KZyf4oiAhcBYs5ad6xshHgQQHpqIPaOAgH2uVYKnD3mXOhSmwItRJ\n" +
            "arLkKP5uJxV8tM8u9mDDoz5vo/zMw1MNv80UV9j+3JrvzWpzkUst/ji2MR7MdLga\n" +
            "5WSroMnNgNJfS/HMXMReGoUjt7jqToP1obotJVd8sm0JfDtgkdYzLSYp6NSlniTd\n" +
            "lTid27kAMAa0w1tCfp+xLd4vXI6hYC8TFwLbYH/lNZPQ8D8Q5FI393pDiuk468pC\n" +
            "p9GmfnvOvR/AQr+NxMSc4XkbUYb1cS9v926qqkt7BJelBhM/pCLf6/LUoVu4oQAw\n" +
            "DQYJKoZIhvcNAQEBBQAEggEAFolxdhoSb0xKo8uShYWfVFGkBnzpSKoBr0GM4bPU\n" +
            "0Yf7afGb1S4df/U7oom/e7PcC244cwqvSlocWIUHCgn93SoAJms6F4vsJzXKQxcM\n" +
            "7+pHR9SAlth/h3/rwsqw4UQV2jB6uH4qxvuJiiJYz4BhZ5yByrXwitD1CvniuKmD\n" +
            "w5schLL6sGr6Fpu0vA4eUI4KZitXAXGDzm1jow8gkhG6Jgkadg6UfZRGuzY521MV\n" +
            "NX95RUXiVlwJw6pbtuEpbmSQsyJGAcx3HSCOQhKgAErg8lwLNjgcZm8Oh/rFUjta\n" +
            "iSh5xFF+rQMdjvmcMgnLAjGWOyVmxbd4dFrn62mTa9Z26wAAAAAAAA==\n" +
            "-----END PKCS7-----\n";

    public static void main(String[] args) throws Exception {
        new RegisterDeviceTest().checkRequest();
        System.exit(0);
    }

    public void checkRequest() throws Exception {
        CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(REQUEST);
        log.info("request: " + cmsSignedMessage.getSignedContentStr());
        OperationCheckerDto operationDto = cmsSignedMessage.getSignedContent(OperationCheckerDto.class);
        RegisterDto registerDto = cmsSignedMessage.getSignedContent(RegisterDto.class);
        log.info("DeviceId: " + registerDto.getDeviceId() + " - SignedContent: " + cmsSignedMessage.getSignedContentStr());
    }

    public void registerDevice() throws Exception {
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(REQUEST.getBytes(), MediaType.PKCS7_SIGNED,
                CurrencyOperation.REGISTER_DEVICE.getUrl(Constants.ID_PROVIDER_ENTITY_ID));
        if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
            RegisterDto registerDto = (RegisterDto) responseDto.getMessage(RegisterDto.class);
            X509Certificate certificate = PEMUtils.fromPEMToX509Cert(registerDto.getIssuedCertificate().getBytes());
            log.info("issued cert: " + certificate);
        } else {
            responseDto = responseDto.getErrorResponse();
            log.info("Error - statusCode: " + responseDto.getStatusCode() + " - " + responseDto.getMessage());
        }

    }

}
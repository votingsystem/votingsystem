package org.currency.test.android;

import org.currency.test.Constants;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.dto.OperationCheckerDto;
import org.votingsystem.dto.OperationTypeDto;
import org.votingsystem.dto.currency.RegisterDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.MediaType;
import org.votingsystem.testlib.BaseTest;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;

import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class RegisterDeviceTest extends BaseTest {

    private static final Logger log = Logger.getLogger(RegisterDeviceTest.class.getName());

    public static final String REQUEST = "-----BEGIN PKCS7-----\n" +
            "MIAGCSqGSIb3DQEHAqCAMIACAQExDzANBglghkgBZQMEAgEFADCABgkqhkiG9w0B\n" +
            "BwGggCSABIID6HsiZGV2aWNlSWQiOiIzN2E0NzUxZi04NjIyLTRlZDgtYmZjNC0z\n" +
            "YmIwZDdlYzMwNWMiLCJtb2JpbGVDc3IiOiItLS0tLUJFR0lOIENFUlRJRklDQVRF\n" +
            "IFJFUVVFU1QtLS0tLVxuTUlJRFJqQ0NBaTRDQVFBd096RVBNQTBHQTFVRUJCTUdS\n" +
            "MEZTUTBsQk1SUXdFZ1lEVlFRcUV3dEtUMU5GSUVwQlxuVmtsRlVqRVNNQkFHQTFV\n" +
            "RUJSTUpNRGMxTlRNeE56SklNSUlCSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVE4\n" +
            "QVxuTUlJQkNnS0NBUUVBMmtSVHY1RE9kcFpHT25VVjNSbmtZS0R4TnBSckJxU0Rx\n" +
            "bXQ3SDFLNXhHZnU1N2dXQXJXU1xuV3lRMzVjQ1o2OVdaQUlKWFNnaTZpaGtFbnpB\n" +
            "Vm9OVmVZOG9NMURIenZkb01pYzNDL1NYcHdrdGREQ1dvTWgxVFxuQmxWSjRnNmU3\n" +
            "dTFUdllKSmRhNEJ2Z0h5ajFSVUlON0NyZHordUFRMFFMcGUrekhNMVc3SUVocXNL\n" +
            "Tkk0WG4zd1xuUjFJUEZLcWkxbGNwWHJtMmxQdm9BSlF6Y1FJN2E3SEduYi9LekhM\n" +
            "alJXeWRvZnZMcEpoRnpSUXI0NWhvOWpkY1xucWt5TmU4UU0weCtRNHE4WmJDTG5L\n" +
            "WE40aEgwZFNwaG56S0p3dEZBRkRoRm5WdnBGZ3gxSkl0RHZ1K0paNVFJdFxuUGU3\n" +
            "Rm81SkROclRlTFNqVEpKK0lSWDhsc2QzbDhaclord0lEQVFBQm9JSEZNSUhDQmdr\n" +
            "QUFBQUFBQUFBQUFReFxuZ2JRTWdiRjdJbVJsZG1salpVNWhiV1VpT2lKamRYSnla\n" +
            "VzVqZVMxdGIySnBiR1V0WVhCd2JHbGpZWFJwYjI0aVxuTENKa1pYWnBZMlZVZVhC\n" +
            "bElqb2lUVTlDU1V4Rklpd2laMmwyWlc1dVlXMWxJam9pU2s5VFJTQktRVlpKUlZJ\n" +
            "aVxuTENKdWRXMUpaQ0k2SWpBM05UVXpNVGN5U0NJc0luTjFjbTVoYldVaU9pSkhR\n" +
            "VkpEU1VFaUxDSjFkV2xrSWpvaVxuTXpkaE5EYzFNV1l0T0RZeU1pMDBaV1E0TFdK\n" +
            "bVl6UXRNMkppTUdRM1pXTXpNRFZqSW4wd0RRWUpLb1pJaHZjTlxuQVFFTEJRQURn\n" +
            "Z0VCQURRcUVvclpCM1RXWTFsa0pSZDZTMHRpaDFGaE5EcnU3VzY4TE0xZm5kVEFh\n" +
            "dFA2RnVFMFxuOElqVjd2UVVKYUdUV2hVQWRHV2hoQi93ZWVXbEE4ekFRYlJuOVl1\n" +
            "WnYEggI5cGt0VXZITGpkOURzYmVJVWVYVWNzenpcbjRjc0M1SmlTM2V0OFRuc3p4\n" +
            "bko5TkY2ZlpubEJ1VUJtam5oY25TYWYydUZlZ2pZcHhTYUVuS3I0RTBESUg5TG9c\n" +
            "blFZUGNMS2lzVXNSeHFSRGJNZENXRVozOG5Sd3pCSEtYeExsZU5hTXBNWkZrWURD\n" +
            "QVJnd012bWUxYU5CUHRMWThcbm5NR1ozUTJLc0dJTjZJb1JIT2hyR3FTTW9uN0pO\n" +
            "QlR6WnZpMjBTUFlXVEFqZ09GTlR5Wmp1MUQvRGRXSDY3ZnBcbkRjaXJZU2srbVVM\n" +
            "NEk2OVJ1VVBDbi9RdXlhbDJWdDE2VXJBPVxuLS0tLS1FTkQgQ0VSVElGSUNBVEUg\n" +
            "UkVRVUVTVC0tLS0tXG4iLCJ1c2VyIjp7ImZ1bGxOYW1lIjoiSk9TRSBKQVZJRVIg\n" +
            "R0FSQ0lBIiwiQ291bnRyeSI6IkdBUkNJQSBaT1JOT1pBLCBKT1NFIEpBVklFUiAo\n" +
            "RklSTUEpIiwiQ2VydGlmaWNhdGVzIjpbeyJyb290IjpmYWxzZX1dLCJHaXZlbk5h\n" +
            "bWUiOiJKT1NFIEpBVklFUiIsIk51bUlkIjoiMDc1NTMxNzJIIiwiU3VyTmFtZSI6\n" +
            "IkdBUkNJQSJ9LCJPcGVyYXRpb24iOnsiRW50aXR5SWQiOiJodHRwczovL3ZvdGlu\n" +
            "Zy5kZG5zLm5ldC9jdXJyZW5jeS1zZXJ2ZXIiLCJUeXBlIjoiUkVHSVNURVIifX0A\n" +
            "AAAAAACggDCCBeQwggTMoAMCAQICEFgD3bb+7psjVhIgg2DRiZ8wDQYJKoZIhvcN\n" +
            "AQEFBQAwXDELMAkGA1UEBhMCRVMxKDAmBgNVBAoMH0RJUkVDQ0lPTiBHRU5FUkFM\n" +
            "IERFIExBIFBPTElDSUExDTALBgNVBAsMBEROSUUxFDASBgNVBAMMC0FDIEROSUUg\n" +
            "MDAxMB4XDTE1MTAwNTA3MDIyN1oXDTIwMTAwNTA3MDIyNlowdjELMAkGA1UEBhMC\n" +
            "RVMxEjAQBgNVBAUTCTA3NTUzMTcySDEPMA0GA1UEBAwGR0FSQ0lBMRQwEgYDVQQq\n" +
            "DAtKT1NFIEpBVklFUjEsMCoGA1UEAwwjR0FSQ0lBIFpPUk5PWkEsIEpPU0UgSkFW\n" +
            "SUVSIChGSVJNQSkwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCZhAwv\n" +
            "7JuGvcNaJkScUVDRcZIm6JOndniK0c+i7VLwhvtYP6mroUzP7iStSjl+8ruZ72Lm\n" +
            "/UlA5kM//QaBFPI68/dEIfoOoPdhZbJ1bRe+RwGMtdN3wsYkLelzqUVDDxgjxvmk\n" +
            "k7O/LJOA34EUTAO2zG5Y+AsMXQk0oo/MeZhP3f7rjuxPMflZGmL8FwMnp1fMpBjE\n" +
            "dTidMBgpddyfj0jNi5n5JVe5vXigRUJLwi+Aw+8dlN/KuFxJ+MB8t1eQbh96wNYi\n" +
            "Mz9RMXFwGzVfk1yvJWb8MsgWhmB9u6EqUfojisldknivNsIDymYxJJuFm++JiBcb\n" +
            "o2ItVBdQcfddNxN/AgMBAAGjggKGMIICgjAMBgNVHRMBAf8EAjAAMB0GA1UdDgQW\n" +
            "BBTFfMGWlSaDbKLYRFvW6Z50ZbFn/DAfBgNVHSMEGDAWgBQaiajF7o92XVVxifM7\n" +
            "Nb2qBQCVbzAiBggrBgEFBQcBAwQWMBQwCAYGBACORgEBMAgGBgQAjkYBBDBgBggr\n" +
            "BgEFBQcBAQRUMFIwHwYIKwYBBQUHMAGGE2h0dHA6Ly9vY3NwLmRuaWUuZXMwLwYI\n" +
            "KwYBBQUHMAKGI2h0dHA6Ly93d3cuZG5pZS5lcy9jZXJ0cy9BQ1JhaXouY3J0MDsG\n" +
            "A1UdIAQ0MDIwMAYIYIVUAQICAgMwJDAiBggrBgEFBQcCARYWaHR0cDovL3d3dy5k\n" +
            "bmllLmVzL2RwYzCB8AYIKwYBBQUHAQIEgeMwgeAwMgIBATALBglghkgBZQMEAgEE\n" +
            "IIxSEvSegOIMR+tChg8zwM4zcwVGjXLHdKDCkulLWco5MDICAQAwCwYJYIZIAWUD\n" +
            "BAIBBCCrWwaBM3z3f0INj+t7Co//tSi5EaQoDthDM/LaP1jfLDA6BglghVQBAgIE\n" +
            "AgEwCwYJYIZIAWUDBAIBBCCM6I8ToupRlRIWRUkELC190RoV4FnzJwmTCMqlBEir\n" +
            "uTA6BglghVQBAgIEAgYwCwYJYIZIAWUDBAIBBCDpMpVoo/Ne6Ni+4Lo/YtLTTWs8\n" +
            "HJvTq3MAzE2NSZP/fTAoBgNVHQkEITAfMB0GCCsGAQUFBwkBMREYDzE5NzEwMzE4\n" +
            "MTIwMDAwWjBCBghghVQBAgIEAQQ2MDQwMgIBAjALBglghkgBZQMEAgEEIJj2G/9j\n" +
            "KaeQlq1+KwrAT3ooChToMcDFwJI8tMKbwjwNMA4GA1UdDwEB/wQEAwIGQDANBgkq\n" +
            "hkiG9w0BAQUFAAOCAQEAdRCj8GYqDAlFnvvS+yjcrIqYWUJ3ge/dzNzQOp1fOSq5\n" +
            "dup7LE5HglD6Twok2rMjf8bul5nZWM/QS0cN6mnrN6WrCWBCfiRf8ZYLsawzQosn\n" +
            "KDebL4FfElgbFQLeIp03CeCY69ESg9crzvCA2tadJ96jETaklb5iRY/6c8/W6qvR\n" +
            "0m0kukdixSlgKb/91TkFtvIzXWkkPXPx8wiZLYwI6OxMbqEZhLCuWfpLy2KbO0Bb\n" +
            "OQ0+ZaDOe5VGpxRwMJlwQNyvPI8ph48SK4RjvFgjW2aYh/jpfZYSVtovCXRP5jPE\n" +
            "pvkrvggrqcPuINR19wAP1YpZF3pa9YP5lJiaqTTcUjCCBcUwggOtoAMCAQICEGQg\n" +
            "ZsmZe67hRALabqQi1kkwDQYJKoZIhvcNAQEFBQAwXTELMAkGA1UEBhMCRVMxKDAm\n" +
            "BgNVBAoMH0RJUkVDQ0lPTiBHRU5FUkFMIERFIExBIFBPTElDSUExDTALBgNVBAsM\n" +
            "BEROSUUxFTATBgNVBAMMDEFDIFJBSVogRE5JRTAeFw0wNjAyMjcxMDU0MzhaFw0y\n" +
            "MTAyMjYyMjU5NTlaMFwxCzAJBgNVBAYTAkVTMSgwJgYDVQQKDB9ESVJFQ0NJT04g\n" +
            "R0VORVJBTCBERSBMQSBQT0xJQ0lBMQ0wCwYDVQQLDARETklFMRQwEgYDVQQDDAtB\n" +
            "QyBETklFIDAwMTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKz+SpnS\n" +
            "5Kf9jQ/ue4y51/uJNX/Omd+kxq78tzLgDVfn7fDWzTsWXKXBkj8OlmGvptMyn4UP\n" +
            "rEBEx4TBzKOxm+p5YagpjU6U3Y9+StAZ+Zmi50xPBfP6f+qU/Qi6xD2FeXl+zmzQ\n" +
            "oqpNr57lkDHFQMEV9O+0GBijc1vfKfSsYquJmGUUdDm58WDXetVg3Mznbj+Qv3VC\n" +
            "KxpNyXxSyDSxldHf/RGnh5e845MzmMgknBqvrZa5ClYFugUMCP4F8OF0WDYRYwgD\n" +
            "82/HGi3r8UMRp80VGfHUzlDnqoDmdRV3zbooOyqHpOKpHckCqMGDaeEtzcHrrr27\n" +
            "GzyWalcwqs8AqvcCAwEAAaOCAYAwggF8MBIGA1UdEwEB/wQIMAYBAf8CAQAwHQYD\n" +
            "VR0OBBYEFBqJqMXuj3ZdVXGJ8zs1vaoFAJVvMB8GA1UdIwQYMBaAFI5F9J9zxf8v\n" +
            "GwXbAUdgGwOKgbe6MA4GA1UdDwEB/wQEAwIBBjA3BgNVHSAEMDAuMCwGBFUdIAAw\n" +
            "JDAiBggrBgEFBQcCARYWaHR0cDovL3d3dy5kbmllLmVzL2RwYzCB3AYDVR0fBIHU\n" +
            "MIHRMIHOoIHLoIHIhiBodHRwOi8vY3Jscy5kbmllLmVzL2NybHMvQVJMLmNybIaB\n" +
            "o2xkYXA6Ly9sZGFwLmRuaWUuZXMvQ049Q1JMLENOPUFDJTIwUkFJWiUyMEROSUUs\n" +
            "T1U9RE5JRSxPPURJUkVDQ0lPTiUyMEdFTkVSQUwlMjBERSUyMExBJTIwUE9MSUNJ\n" +
            "QSxDPUVTP2F1dGhvcml0eVJldm9jYXRpb25MaXN0P2Jhc2U/b2JqZWN0Y2xhc3M9\n" +
            "Y1JMRGlzdHJpYnV0aW9uUG9pbnQwDQYJKoZIhvcNAQEFBQADggIBAGdY/2uyHzpn\n" +
            "Cf4HqyLlGJwRJOh25dOaY0k7Wjn97fAsUufeONInnRg3dm7gGx1aFJSt3eEPJ79K\n" +
            "rLNFzJ+graO+QhZ9hIcsWXpOng425gVHCu1TxFpIzHUGVIwyMWmuJn28TBgPgcdk\n" +
            "n39M43HbLsRJw82F5q/ZZT+36l/PPezEWqCj+SpO2GnLuPY67T1GEwrQB7p2Q8rb\n" +
            "Bkji7fE4RQ2ovJCLPB3/HYibwVBo8vRZUnqKn4rgL4HTybVZzXncbRHqOf6BnN/v\n" +
            "S9NjxqqOKD4MbRQnCdjpbDL4eY8X/Flv6HJJ1bqTbXmyqtO9kO9qCyAsvb2kQrec\n" +
            "Rk/pVNKEWF0/jeNXnh2FuUhzElas23NrieKWMTBZY08eYj9DbCrDsDeoq/zspx7B\n" +
            "zWwvYJY9dD4kkJDHVPKg8oG3/+DhWMM/+EaM4WjFMw9mF7YjqRfFOUcnohBw2dJQ\n" +
            "pCeqRre3Biz4S8s6mHfIs2E7VFVzZiRq4+eH9QBuFCosfSMPF89VsgI20ro/GZ/O\n" +
            "BT8iK7ICLZX5LRkoDMRdfOW2s8ULR8qL+XgaLIvR/jwZfgj3kNtzsybQiuBbLHaZ\n" +
            "T3TAQHh2hiJ8S2ab7daoyZzSLNEGlysh4+j3KDdTA4GKOclgI9WTxeEocYlOkwRx\n" +
            "/UscLbTZsZChZDWBcE95yqvY+uikdwtnAAAxggjaMIII1gIBATBwMFwxCzAJBgNV\n" +
            "BAYTAkVTMSgwJgYDVQQKDB9ESVJFQ0NJT04gR0VORVJBTCBERSBMQSBQT0xJQ0lB\n" +
            "MQ0wCwYDVQQLDARETklFMRQwEgYDVQQDDAtBQyBETklFIDAwMQIQWAPdtv7umyNW\n" +
            "EiCDYNGJnzANBglghkgBZQMEAgEFAKCCBzswGAYJKoZIhvcNAQkDMQsGCSqGSIb3\n" +
            "DQEHATAcBgkqhkiG9w0BCQUxDxcNMTcwNDAzMTA1MTQ3WjAvBgkqhkiG9w0BCQQx\n" +
            "IgQgU1eQ/0wHqujgWogprp5UPwInWIVmtpGdcfupT8rhFhIwggbOBgsqhkiG9w0B\n" +
            "CRACDjGCBr0wgga5BgkqhkiG9w0BBwKgggaqMIIGpgIBAzEPMA0GCWCGSAFlAwQC\n" +
            "AQUAMHkGCyqGSIb3DQEJEAEEoGoEaDBmAgEBBgaCEoQ3hnowMTANBglghkgBZQME\n" +
            "AgEFAAQgU1eQ/0wHqujgWogprp5UPwInWIVmtpGdcfupT8rhFhICCCrIHE7f5pic\n" +
            "GA8yMDE3MDQwMzA4NTEzMlowCwIBAYACAfSBAgH0oIID1jCCA9IwggK6oAMCAQIC\n" +
            "BgFbGvFaMjANBgkqhkiG9w0BAQsFADAsMRowGAYDVQQDDBFGQUtFIFJPT1QgRE5J\n" +
            "ZSBDQTEOMAwGA1UECwwFQ2VydHMwHhcNMTcwMzI5MTQ0MTE2WhcNMTgwMzI5MTQ0\n" +
            "MTE2WjArMSkwJwYDVQQqEyBWb3RpbmctQ3VycmVuY3kgVGltZXN0YW1wIFNlcnZl\n" +
            "cjCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAIKkn9q4bqQYFm/wM7zT\n" +
            "bXaoqbjEoeMrO7ACgxAJRcTYKjGFOUJfH3joQ/bnxNyAumV+X8KukM9VbDW10dgY\n" +
            "JyiKiKKsh71PLiaU8fv7WyP7uHJ4ScwJ6Lo+mlFkuzYo1ZlmGwUK6RsQ8VdQ2JMM\n" +
            "pU3oFrGa1WA3r4cIpKhdUHU+muaKdLzdZsBbmDeEs/whwMjeyMiJouQGY1LO3TPd\n" +
            "HUDNCpkF/HHpfORSkPeTijsdolpWHz1J+iw1EEW5ROH1h2RcqSmbWpP/ZJpw3e0F\n" +
            "MiECAjQDhaXzihevRSZ8sYowRtjUz/+zIQBJvd5PiGMGZjgwlaBfGn3LAD1IvS9C\n" +
            "wLcCAwEAAaOB+jCB9zBDBggrBgEFBQcBAQQ3MDUwMwYIKwYBBQUHMAGGJ2h0dHBz\n" +
            "Oi8vdm90aW5nLmRkbnMubmV0L2lkcHJvdmlkZXIvb2NzcDBbBgNVHSMEVDBSgBQ9\n" +
            "mNuHs0MLYMMDMD1iarJr0LxcMKEwpC4wLDEaMBgGA1UEAwwRRkFLRSBST09UIERO\n" +
            "SWUgQ0ExDjAMBgNVBAsMBUNlcnRzgghvzDD4vCDU6jAdBgNVHQ4EFgQUoW39F3vf\n" +
            "AC49S9p6TXDtUgmArIIwDAYDVR0TAQH/BAIwADAOBgNVHQ8BAf8EBAMCBaAwFgYD\n" +
            "VR0lAQH/BAwwCgYIKwYBBQUHAwgwDQYJKoZIhvcNAQELBQADggEBAJ3FI/flbzJ9\n" +
            "pbWbnCLwy7JiCG0POrfqZo/s4jVkj0WjEEEGf02uxeGmJtj7SDiOYPFIUvn0953N\n" +
            "eKV5O6NsKA50JBv+nytiMBUNyn6Ctmg1Os6IRxsewT2D7rM2I/Cv6R4484OTLNe2\n" +
            "Hy+OQyLv7Q4I+jtGg/bYxbAH7C9d2hWHwcgzS3mtz4MyvZwTMRLTDCMZwpoIkpDR\n" +
            "nfinCcX0asDPSRRfsVFW2g/u7dZXQkJmD9qcraxH7WtFrRlD4RVFnlr9lhid65bW\n" +
            "GUjYtR6v5OsSYuxj1iMaoVeSgDDgY4snbQacquv8nWSW+ADSIbhI0bMAMm1ZY5a0\n" +
            "HDAUJoxcmUYxggI5MIICNQIBATA2MCwxGjAYBgNVBAMMEUZBS0UgUk9PVCBETkll\n" +
            "IENBMQ4wDAYDVQQLDAVDZXJ0cwIGAVsa8VoyMA0GCWCGSAFlAwQCAQUAoIHTMBoG\n" +
            "CSqGSIb3DQEJAzENBgsqhkiG9w0BCRABBDAcBgkqhkiG9w0BCQUxDxcNMTcwNDAz\n" +
            "MDg1MTMyWjAtBgkqhkiG9w0BCTQxIDAeMA0GCWCGSAFlAwQCAQUAoQ0GCSqGSIb3\n" +
            "DQEBAQUAMC8GCSqGSIb3DQEJBDEiBCAh+7Ov8i3p2ZMWpX7Ld25G4UaU+sru7Rzy\n" +
            "vOTnOw4mbzA3BgsqhkiG9w0BCRACLzEoMCYwJDAiBCA6aOW3NOxOOBtTCU48/YU9\n" +
            "gfiwcufwEzKMHW3Cb1JQFDANBgkqhkiG9w0BAQEFAASCAQBxNaurWDh7tg+t4scJ\n" +
            "l3iV0fdHFfDK9GV/ttQmCvsX3c7kqskuKMSHi2TAxjWao+/gt2MbisewvWUKpcTB\n" +
            "RI5eEx0lipsYF+TU9pw70JR/oMM9KAyxCxyRPWUPCkvVm1oGmttB6NjN8TmRvXWO\n" +
            "jYJND1s9fOZAXE3zIvz6cpxzbiEP+oFTYyrLy0Qc+zf9Sw1K5CE2utVEomRmNWjp\n" +
            "AK43aN6ksfWK+9UVEiHualriE1uNrIxF2lJTVHc5xDannwYt6IQ9S1RqzoBsMOgP\n" +
            "Oxejb+T/0Fb7JxkXbh02yhkiDQrxYRkbZbXZJPJ0Kve/u6KalWir2zS4vXEFLzB7\n" +
            "4WEaoQAwDQYJKoZIhvcNAQEBBQAEggEAV/3UaRtphDq0hHMzGC/uKTUNwcDkXExA\n" +
            "VcqpJyxVVnuoFcV2PsO6sojB3ya9zhOotwmo0tTbvV0rKuegPKADMuqa8GAHcUXS\n" +
            "0r89XjhkIkqhl9tRY5YqoXEYENk2FzZSlkcJxpsAKnHMYEEfYHEBmhPZxNDeoc2+\n" +
            "K6aUezxACywIX7hZvXCAqCjxJs2aqzkVrSRpyjuUgWlNuyzITeuOc6OxgKjcUmv3\n" +
            "JL8uJLROTcydKUxoKPrMwVFYP4AV9wOrJSUr3UOqbvZIrX+ThphAnexgtAcmw98E\n" +
            "mqqqAboma/Km/Ngjy3B8r2a5AJ4c3wBHqzp3ZrMgGyGwHiuPdNAVnAAAAAAAAA==\n" +
            "-----END PKCS7-----";
    public static void main(String[] args) throws Exception {
        new RegisterDeviceTest().registerDevice();
        System.exit(0);
    }

    public void checkOperationCheckerDto() throws Exception {
        OperationCheckerDto operationDto = new OperationCheckerDto();
        operationDto.setOperation(new OperationTypeDto(CurrencyOperation.REGISTER,"entityId"));
        log.info("operationDto: " + JSON.getMapper().writeValueAsString(operationDto));
    }

    public void checkRequest() throws Exception {
        CMSSignedMessage cmsSignedMessage = CMSSignedMessage.FROM_PEM(REQUEST);
        OperationCheckerDto operationDto = cmsSignedMessage.getSignedContent(OperationCheckerDto.class);
        RegisterDto registerDto = cmsSignedMessage.getSignedContent(RegisterDto.class);
        log.info("DeviceId: " + registerDto.getDeviceId() + " - SignedContent: " + cmsSignedMessage.getSignedContentStr());
    }

    public void registerDevice() {
        ResponseDto responseDto = HttpConn.getInstance().doPostRequest(REQUEST.getBytes(), MediaType.PKCS7_SIGNED,
                CurrencyOperation.REGISTER.getUrl(Constants.CURRENCY_SERVICE_ENTITY_ID));
        log.info("status: " + responseDto.getStatusCode() + " - message: " + responseDto.getMessage());
    }

}
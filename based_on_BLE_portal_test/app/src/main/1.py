import requests, time

t = time.localtime()
year = str(t.tm_year)
month = str("%02d"%t.tm_mon)
day = str("%02d"%t.tm_mday)
hour = str("%02d"%t.tm_hour)
sec = t.tm_sec
m = t.tm_min

filename = input("which property? ;")
f = open(filename+'.txt','r')
s = eval(f.read())
f.close()

host = s.get('host')
accid = s.get('accid')
permoney = "5000"
devid = s.get('devid')
prjid = s.get('prjid')
logincode = s.get('logincode')
upmoney = s.get('upmoney')
devtype = s.get('devtype')
groupid = s.get('groupid')

um = input("how much woulid you like to pay for this shower?")
if um != '':
    try:
        upmoney = um
    except Exception:
        pass
    finally:
        pass
 
header = {
"Content-Length":"0",
"Host":"118.31.18.116",
"Connection":"Keep-Alive",
"Accept-Encoding":"gzip",
"User-Agent":"okhttp/3.4.2",
}
     
def jiezhang(ConsumDT):
    url_consum = "http://%s/appI/api/savexf?AccID=%s&ConsumeDT=%s&DevID=%s&GroupID=%s&PerMoney=%s&PrjID=%s&UpMoney=%s&devType=%s&loginCode=%s%2C508487&phoneSystem=ios&version=1.0.1"%(host, accid, ConsumDT, devid,  groupid, permoney, prjid, upmoney, devtype, logincode)
    print(url_consum)
    response = requests.post(url_consum,headers=header).content
    result = str(response,"utf8")
    print(result)
    result = eval(result)
    if result.get("error_code")=="0":
        print(result.get("error_code"))
        print("结账成功")
        return 1
    else:
        print("failed to pay....retrying.........")
        return 0
    sec_bak = sec

while True:
    second = "%02d"%sec
    minute = str("%02d"%m)
    ConsumDT = year+month+day+hour+minute+second
    if jiezhang(ConsumDT):
        break
    sec -=1
    if sec < 0:
        sec = 60
    m -= 1
    if sec == sec_bak:
        break
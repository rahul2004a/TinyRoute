"""Run the existing local redirect protocol with anonymous capture enabled.

Requires the passing Maven suite, Docker, trusted local TLS files, and the
recorded runtime images from the existing link performance setup.
"""
from pathlib import Path
import datetime
import hashlib
import json
import os
import secrets
import subprocess
import time
import xml.etree.ElementTree as ET

root = Path.cwd()
private = root / '.local-verification' / ('analytics-' + datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ'))
private.mkdir(mode=0o700, parents=True)
metadata = json.loads((root / 'docs/spec/link-creation-and-redirection/performance-environment.json').read_text())
report = next((root / 'backend/target/failsafe-reports').glob('TEST-*.xml'))
properties = {p.attrib['name']: p.attrib['value'] for p in ET.parse(report).getroot().findall('./properties/property')}
classpath = properties.get('surefire.test.class.path') or properties['java.class.path']
token = secrets.token_urlsafe(48)
backend = 'tinyroute-analytics-perf-' + secrets.token_hex(4)
proxy = backend + '-proxy'
state = {'private': str(private), 'backend': backend, 'proxy': proxy}
(private / 'state.json').write_text(json.dumps(state))
(root / '.local-verification/analytics-current.txt').write_text(str(private))
values = {
    'SPRING_PROFILES_ACTIVE': 'dev', 'DOCKER_HOST': 'unix:///var/run/docker.sock',
    'TESTCONTAINERS_HOST_OVERRIDE': 'host.docker.internal',
    'TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE': '/var/run/docker.sock',
    'TINYROUTE_VERIFICATION_TOKEN': token, 'TINYROUTE_VERIFICATION_PORT': '8444',
    'MAIL_PORT': '1026', 'ALLOWED_FRONTEND_ORIGINS': 'https://localhost:3001',
    'PASSWORD_RESET_CONFIRMATION_URI': 'https://localhost:3001/password-reset/confirm',
    'DEV_TLS_CERTIFICATE': f'file:{root}/.local-certs/localhost.pem',
    'DEV_TLS_PRIVATE_KEY': f'file:{root}/.local-certs/localhost-key.pem',
    'ANALYTICS_GEO_DATABASE_PATH': str(root / 'backend/src/test/resources/geo/GeoIP2-City-Test.mmdb'),
}
envfile = private / 'backend.env'
envfile.write_text(''.join(f'{key}={value}\n' for key,value in values.items()))
envfile.chmod(0o600)
relay = private / 'tcp-proxy.mjs'
relay.write_text('''import net from 'node:net';
const server=net.createServer(client=>{
 const upstream=net.connect({host:'127.0.0.1',port:8444,localAddress:'127.0.0.2'});
 client.on('error',()=>upstream.destroy());upstream.on('error',()=>client.destroy());
 client.pipe(upstream);upstream.pipe(client);
 client.on('close',()=>upstream.destroy());upstream.on('close',()=>client.destroy());
});server.listen(9444,'0.0.0.0');
''')
environment=os.environ.copy()
environment['NODE_EXTRA_CA_CERTS']=str(Path.home()/'Library/Application Support/mkcert/rootCA.pem')
environment['TINYROUTE_VERIFICATION_TOKEN']=token
probe=private/'probe.mjs'
probe.write_text('''import https from 'node:https';
const path=process.argv[2]??'/actuator/health';
const request=https.get({hostname:'localhost',port:8444,path,localAddress:'127.0.0.1',family:4,rejectUnauthorized:true,headers:{'X-Verification-Token':process.env.TINYROUTE_VERIFICATION_TOKEN},signal:AbortSignal.timeout(5000)},response=>{
 const chunks=[];response.on('data',c=>chunks.push(c));response.on('end',()=>{
 if(response.statusCode!==200)process.exit(2);const body=JSON.parse(Buffer.concat(chunks));
 if(path==='/actuator/health'&&body.status!=='UP')process.exit(3);
 if(path==='/__verification/analytics-state')console.log(JSON.stringify(body));
 });});request.on('error',()=>process.exit(1));
''')
def probe_state(path):
    return subprocess.run(['node',str(probe),path],env=environment,capture_output=True,text=True)
try:
    subprocess.run(['docker','run','-d','--name',backend,'--label','com.tinyroute.verification=analytics-performance','--user','0:0','--network','bridge','--publish','127.0.0.1:8444:9444','--entrypoint','java','--workdir',str(root/'backend'),'--env-file',str(envfile),'--mount',f'type=bind,source={root},target={root},readonly','--mount',f'type=bind,source={Path.home()}/.m2/repository,target={Path.home()}/.m2/repository,readonly','--mount','type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock',metadata['backendRuntimeImage'],'-cp',classpath,'com.tinyroute.LinkVerificationApplication'],check=True,capture_output=True)
    subprocess.run(['docker','run','-d','--name',proxy,'--label','com.tinyroute.verification=analytics-performance','--network','container:'+backend,'--user',f'{os.getuid()}:{os.getgid()}','--entrypoint','node','--mount',f'type=bind,source={relay},target=/work/relay.mjs,readonly',metadata['proxyRuntimeImage'],'/work/relay.mjs'],check=True,capture_output=True)
    print('Started isolated analytics verification runtime.',flush=True)
    for attempt in range(60):
        if probe_state('/actuator/health').returncode==0 and probe_state('/__verification/bootstrap').returncode==0:break
        time.sleep(2)
    else:raise RuntimeError('Verified HTTPS/fixture preflight failed; load not started.')
    print('Verified HTTPS health and guarded fixture. Starting 100 rps for 600 seconds.',flush=True)
    command=['/usr/bin/caffeinate','-i','node','tools/link-performance.mjs','--via-local-proxy','--api-base','https://localhost:8444','--rate','100','--duration','600','--warmup','30','--create-samples','200','--clients','100','--output',str(private/'link-performance.json')]
    (private/'command.json').write_text(json.dumps(command))
    metadata['harnessSha256']=hashlib.sha256((root/'tools/link-performance.mjs').read_bytes()).hexdigest()
    classes=root/'backend/target/classes'
    class_hash=hashlib.sha256()
    for file in sorted(classes.rglob('*.class')):
        class_hash.update(str(file.relative_to(classes)).encode());class_hash.update(file.read_bytes())
    metadata['backendClassesSha256']=class_hash.hexdigest()
    metadata['analyticsEnabled']=True
    metadata['syntheticGeographyEnabled']=True
    metadata['featureCommit']=subprocess.run(['git','rev-parse','HEAD'],capture_output=True,text=True,check=True).stdout.strip()
    metadata['implementationDiffSha256']=hashlib.sha256(subprocess.run(['git','diff','--binary','HEAD'],capture_output=True,check=True).stdout).hexdigest()
    metadata['recordedAt']=datetime.datetime.now(datetime.timezone.utc).isoformat()
    samples=[]
    with (private/'driver.log').open('w') as log:
        driver=subprocess.Popen(command,cwd=root,env=environment,stdout=log,stderr=subprocess.STDOUT)
        while driver.poll() is None:
            snapshot=probe_state('/__verification/analytics-state')
            if snapshot.returncode==0:samples.append(json.loads(snapshot.stdout))
            time.sleep(10)
    deadline=time.monotonic()+60
    while True:
        snapshot=probe_state('/__verification/analytics-state')
        if snapshot.returncode!=0:raise RuntimeError('Final capture probe failed.')
        final=json.loads(snapshot.stdout)
        if driver.returncode!=0 or final['events']>=63000 or time.monotonic()>=deadline:break
        time.sleep(1)
    samples.append(final)
    (private/'analytics-capture.json').write_text(json.dumps({'samples':samples,'final':final,'maxRecordingDelayMillis':max(s['maxRecordingDelayMillis'] for s in samples)},indent=2))
    if driver.returncode!=0:
        metadata['acceptanceStatus']='FAILED: original driver rejected the protocol; inspect driver.log'
        (private/'environment.json').write_text(json.dumps(metadata,indent=2))
        raise RuntimeError('Existing performance driver failed before acceptance; private evidence retained.')
    data=json.loads((private/'link-performance.json').read_text())
    passed=data['passed'] and final['events']==63000 and final['lifetime']==63000 and not any(final[k] for k in ['failed','rejected','stale']) and max(s['maxRecordingDelayMillis'] for s in samples)<60000
    metadata['acceptanceStatus']='PASSED: local protocol and capture' if passed else 'FAILED: inspect numeric evidence'
    (private/'environment.json').write_text(json.dumps(metadata,indent=2))
    print(json.dumps({'passed':passed,'creationP95':data['creation']['p95'],'redirectP95':data['redirect']['p95'],'redirectP99':data['redirect']['p99'],'capture':final}),flush=True)
    if not passed:raise RuntimeError('Performance/capture acceptance failed.')
finally:
    subprocess.run(['docker','stop',proxy,backend],capture_output=True)
    subprocess.run(['docker','rm',proxy,backend],capture_output=True)

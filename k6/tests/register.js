import http from 'k6/http';
import {check, sleep} from 'k6';

export const options = {
    stages: [
        {duration: '10s', target: 10},  // sobe para 10 usuários
        {duration: '30s', target: 10},  // mantém por 30s
        {duration: '10s', target: 0},   // desce
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'], // 2s é tempo suficiente pro Argon trabalhar
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const payload = JSON.stringify({
        fullName: `User Test ${__VU}-${__ITER}`,
        email: `user_${__VU}_${__ITER}@test.com`,
        password: 'Senha@123',
        birthDate: '1995-06-15',
    });

    const headers = {'Content-Type': 'application/json'};

    const res = http.post('http://localhost:8080/users/register', payload, {headers});

    console.log(`Status: ${res.status} | Body: ${res.body}`);

    check(res, {
        'status 201': (r) => r.status === 201,
        'tempo < 500ms': (r) => r.timings.duration < 500,
    });

    sleep(1);
}
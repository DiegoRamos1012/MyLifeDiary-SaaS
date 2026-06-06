import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.K6_BASE_URL || 'http://localhost:8080';

export const options = {
    stages: [
        { duration: '10s', target: 10 },
        { duration: '30s', target: 10 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'],
        http_req_failed: ['rate<0.01'],
    },
};

function registerUser(email, password, fullName) {
    const payload = JSON.stringify({
        fullName,
        email,
        password,
        birthDate: '1995-06-15',
    });

    return http.post(`${BASE_URL}/users/register`, payload, {
        headers: { 'Content-Type': 'application/json' },
    });
}

function loginUser(email, password) {
    const payload = JSON.stringify({ email, password });

    return http.post(`${BASE_URL}/auth/login`, payload, {
        headers: { 'Content-Type': 'application/json' },
    });
}

function createHabit(userId, accessToken) {
    const payload = JSON.stringify({
        title: 'Ler 10 páginas',
        description: 'Leitura diária para estudo',
        category: 'STUDY',
        goalDaily: 1,
        startDate: '2026-06-05',
    });

    return http.post(`${BASE_URL}/habits/users/${userId}`, payload, {
        headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${accessToken}`,
        },
    });
}

export default function () {
    const email = `user_${__VU}_${__ITER}@test.com`;
    const password = 'Senha@123';
    const fullName = `User Test ${__VU}-${__ITER}`;

    const registerRes = registerUser(email, password, fullName);
    console.log(`register => status: ${registerRes.status} | body: ${registerRes.body}`);

    check(registerRes, {
        'register status 201': (r) => r.status === 201,
    });

    let userId = null;
    try {
        const registerBody = registerRes.json();
        userId = registerBody?.id || null;
    } catch (e) {
        // ignore parse errors and keep userId null
    }

    const loginRes = loginUser(email, password);
    console.log(`login => status: ${loginRes.status} | body: ${loginRes.body}`);

    check(loginRes, {
        'login status 200': (r) => r.status === 200,
        'login returned accessToken': (r) => {
            try {
                return Boolean(r.json('accessToken'));
            } catch (e) {
                return false;
            }
        },
    });

    let accessToken = null;
    try {
        const loginBody = loginRes.json();
        accessToken = loginBody?.accessToken || null;
    } catch (e) {
        // ignore parse errors and keep accessToken null
    }

    if (userId && accessToken) {
        const habitRes = createHabit(userId, accessToken);
        console.log(`create habit => status: ${habitRes.status} | body: ${habitRes.body}`);

        check(habitRes, {
            'habit status 201': (r) => r.status === 201,
        });
    } else {
        console.log('Skipping authenticated habit creation because userId or accessToken is missing.');
    }

    sleep(1);
}


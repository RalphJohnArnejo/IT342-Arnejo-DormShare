import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

function OAuth2Callback({ onLogin }) {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()

  useEffect(() => {
    const token = searchParams.get('token')
    const userId = searchParams.get('userId')
    const email = searchParams.get('email')
    const firstName = searchParams.get('firstName')
    const lastName = searchParams.get('lastName')
    const role = searchParams.get('role')

    if (token) {
      onLogin(token, { token, userId, email, firstName, lastName, role })
      navigate('/dashboard')
    } else {
      navigate('/login')
    }
  }, [searchParams, onLogin, navigate])

  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: '#fdfbf5' }}>
      <p style={{ color: '#6b7280', fontSize: '1rem' }}>Signing you in...</p>
    </div>
  )
}

export default OAuth2Callback

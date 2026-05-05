import React, { useEffect } from 'react'
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
      const userData = { token, userId, email, firstName, lastName, role }
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(userData))
      
      if (onLogin) {
        onLogin(token, userData)
      }
      
      // Redirect immediately to dashboard
      navigate('/dashboard', { replace: true })
    } else {
      // Check if user is already logged in
      const existingToken = localStorage.getItem('token')
      if (existingToken) {
        navigate('/dashboard', { replace: true })
      } else {
        navigate('/login', { replace: true })
      }
    }
  }, [searchParams, onLogin, navigate])

  // Don't render anything - just redirect
  return null
}

export default OAuth2Callback

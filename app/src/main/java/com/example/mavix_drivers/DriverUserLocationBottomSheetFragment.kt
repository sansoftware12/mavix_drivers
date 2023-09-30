package com.example.mavix_drivers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DriverUserLocationBottomSheetFragment : BottomSheetDialogFragment() {
    companion object {
        private const val ARG_EMAIL = "email"
        private const val ARG_PHONE = "phone"
        private const val ARG_DISTANCE = "distance"

        fun newInstance(email: String, phone: String, distance: String): DriverUserLocationBottomSheetFragment {
            val fragment = DriverUserLocationBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL, email)
            args.putString(ARG_PHONE, phone)
            args.putString(ARG_DISTANCE, distance)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_driver_user_location_bottom_sheet, container, false)

        val textEmail: TextView = view.findViewById(R.id.textEmail)
        val textPhone: TextView = view.findViewById(R.id.textPhone)
        val textDistance: TextView = view.findViewById(R.id.textDistance)

        arguments?.let {
            val email = it.getString(ARG_EMAIL)
            val phone = it.getString(ARG_PHONE)
            val distance = it.getString(ARG_DISTANCE)

            textEmail.text = email
            textPhone.text = phone
            textDistance.text = distance
        }

        return view
    }
}

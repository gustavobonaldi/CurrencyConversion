package br.com.btg.btgchallenge.presentation.conversions

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import br.com.btg.btgchallenge.R
import br.com.btg.btgchallenge.databinding.FragmentConversionsBinding
import br.com.btg.btgchallenge.network.api.config.Resource
import br.com.btg.btgchallenge.network.api.config.Status
import br.com.btg.btgchallenge.presentation.FragmentBase
import br.com.btg.btgchallenge.presentation.conversions.Currency.CurrencyType
import br.com.btg.btgchallenge.presentation.currencylist.FragmentCurrencyList
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_conversions.*
import org.koin.android.viewmodel.ext.android.viewModel
import java.text.NumberFormat
import java.util.*

class FragmentConversions : FragmentBase() {

    val conversionViewModel: ConversionViewModel by viewModel()
    private lateinit var binding: FragmentConversionsBinding
    private lateinit var fragmentCurrencyList: FragmentCurrencyList
    private var currencyFrom = Currency(Pair("", ""), CurrencyType.FROM)
    private var currencyTo = Currency(Pair("", ""), CurrencyType.TO)
    private var valueToConvert: Double = 0.0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_conversions, container, false)
        binding.viewModel = conversionViewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        setObservers()
        conversionViewModel.getRealtimeRates()
    }

    fun setListeners(){
        container_from.setOnClickListener{
            showCurrencyList(CurrencyType.FROM)
        }

        container_to.setOnClickListener {
            showCurrencyList(CurrencyType.TO)
        }

        currency_from_edit_text.addTextChangedListener(convertCurrency())
    }

    fun showCurrencyList(currencyType: CurrencyType) {
        fragmentCurrencyList = FragmentCurrencyList(currencyType) {
            if (currencyType == CurrencyType.FROM) {
                clearFields()
                currencyFrom = it
                setDrawableFlag(image_view_from, it)
                text_view_from.text = it.currency.first + " - " + it.currency.second
            } else if (currencyType == CurrencyType.TO) {
                clearFields()
                currencyTo = it
                setDrawableFlag(image_view_to, it)
                text_view_to.text = it.currency.first + " - " + it.currency.second
            }
            fragmentCurrencyList.dismiss()
        }
        fragmentCurrencyList.show(activity?.supportFragmentManager!!, "teste")
    }

    fun setDrawableFlag(imageView: ImageView, currency: Currency) {
        val uri = "@drawable/flag_" + currency.currency.first.toString().toLowerCase()
        var imageResource: Int =
            requireContext().resources.getIdentifier(uri, null, requireContext().getPackageName())
        if (imageResource == 0) {
            imageResource = R.drawable.globe
        }
        imageView.setImageResource(imageResource)
    }

    fun setObservers(){
        conversionViewModel.realTimeRatesLiveData.observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.SUCCESS -> {

                }
                Status.ERROR -> {
                    showSnackbar(it?.message)
                }
                Status.LOADING -> {
                }
            }
        })
    }

    private fun setValueConverted(value: Double) {
        valueToConvert = value
        try {
            conversionViewModel.getConversionFromTo(
                currencyFrom.currency,
                currencyTo.currency,
                ((value).toString().toDouble())
            ) {
                showSnackbar(resources.getString(it.errorMessage))
            }
        } catch (ex: Exception) {
            when {
                ex is NumberFormatException -> {
                    showSnackbar(resources.getString(R.string.type_valid_value))
                }
            }
        }
    }

    private var current = ""
    fun convertCurrency(): TextWatcher {
        return object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
                var s = s

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.toString().equals(current)) {
                    currency_from_edit_text.removeTextChangedListener(this)
                    val cleanString = s?.filter { it.isDigit() }
                    if (!cleanString?.isEmpty()!!) {
                        val parsed = java.lang.Double.parseDouble(cleanString.toString())
                        setValueConverted(parsed / 100)
                        val formatted =
                            NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))
                        current = formatted
                        currency_from_edit_text.setText(formatted)
                        currency_from_edit_text.setSelection(formatted.length)
                    }
                    currency_from_edit_text.addTextChangedListener(this)
                }
            }
        }
    }

    fun showSnackbar(message: String?) {
        try {
            message?.let { Snackbar.make(currency_from_edit_text, it, Snackbar.LENGTH_LONG).show() }
        } catch (ex: Exception) {

        }
    }

    fun clearFields() {
        text_view_converted_value.text = ""
        currency_from_edit_text.setText("")
    }
}
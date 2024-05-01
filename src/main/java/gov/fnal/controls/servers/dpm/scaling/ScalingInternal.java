// $I$
package gov.fnal.controls.servers.dpm.scaling;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static gov.fnal.controls.db.DbServer.getDbServer;

// Multifunction diapason

class TransformTableData
{
    int		table_number;		// table number 1,2,...
    int		order_number;		// diapason order number 0,1,.... 
    int		common_transform;	// common transform for the diapason
    double	primary_initial;	// primary units diapason lower limit
    double	primary_final;		// primary units diapason upper limit
    double	common_initial;		// common units diapason lower limit
    double	common_final;		// common units diapason upper limit
    double []	cx;			// coefficients for the transform

    TransformTableData ()
	{
		this.table_number    = 0;	// table number 1,2,...
		this.order_number    = 0;	// diapason order number 0,1,.... 
		this.common_transform= 0;	// common transform for the diapason
		this.primary_initial = 0;	// primary units diapason lower limit
		this.primary_final   = 0;	// primary units diapason upper limit
		this.common_initial  = 0;	// common units diapason lower limit
		this.common_final    = 0;	// common units diapason upper limit
		this.cx = new double[10];	// coefficients for the transform
	}
}

abstract class ScalingInternal
{
    static final int INTERPOLATE_LINEAR = 0; // perform a linear
                                                // interpolation

    static final int INTERPOLATE_EXP = 1; // perform an exponential
                                            // interpolation

    static final short N_TABLES_MAX = 50; // maximum number of interpolation tables

    static final short MAX_TABLE_SIZE = 100; // maximum number of elements per table

    static final short N_MFCT_TABLES_MAX = 50; // maximum number of multifunction tables

    static final short MAX_MFCT_TABLE_SIZE = 10; // maximum number of elements per table

    static int n_interpolation_tables = 0;   // both linear and logarithmic

    static int n_mfc_transform_tables = 0;  //multifunction common transforms

    static int [] table_size = new int[N_TABLES_MAX];	// interpolation tables
	
    static int [] mfct_table_size = new int[N_MFCT_TABLES_MAX];// multifunction tables

    static String dbt_interp = "accdb..scaling_interpolation_table_values";
    static String dbt_interp_P = "accdb.scaling_interpolation_table_values";

    static String dbt_multi = "accdb..scaling_multifunction_transforms";	
    static String dbt_multi_P = "accdb.scaling_multifunction_transforms";	

    static double[] table_primary_values = null;
    static double[] table_common_values = null;

    static TransformTableData[] all_transform_values = null;

    /***************************************************************************
    *   Reading all interpolation tables from the Data Base table
    *	n_interpolation_tables = the local variable contains the number of
    *		   all interpolation tables
    *	before the first need of any interpolation table
    *	n_interpolation_tables = 0, after that the function downloads all
    *		   interpolation tables into the memory.
    *
    ****************************************************************************/
    static void read_interpolation_tables()
	{
		int n_table_lines;
		int il, jl, kl, nl;
		int vpointer;	// work variable for loops

		if (n_interpolation_tables != 0)
			return;

		// calculate the number of interpolation tables and the table sizes

		for (il = 0; il < N_TABLES_MAX; il++) table_size[il] = 0;
		n_table_lines = 0;		// initial value
		try {
			final ResultSet table_elements = getDbServer("adbs").executeQuery("SELECT table_number FROM "+dbt_interp_P);

			while (table_elements.next()) {	
				il= table_elements.getInt("table_number");
				table_size[il-1]++;
				if (table_size[il-1] == 1)
					n_interpolation_tables++;
				n_table_lines++;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		table_primary_values = new double[n_table_lines];
		table_common_values  = new double[n_table_lines];


		// read the primary values

		vpointer = 0;
		for (il = 0; il < n_interpolation_tables; il++ ) {	
			jl = il+1;		// table index
			try {
				final ResultSet table_primary = getDbServer("adbs").executeQuery("SELECT primary_value FROM "+ dbt_interp_P
													+ " WHERE table_number=" + jl + " order by order_number");
				while (table_primary.next() ) {	
					table_primary_values[vpointer] = table_primary.getDouble("primary_value");
					vpointer++;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		// read the common values
		vpointer = 0;
		for (il = 0; il < n_interpolation_tables; il++ ) {	
			jl = il+1;		// table index
			try {
				final ResultSet table_common = getDbServer("adbs").executeQuery("SELECT common_value FROM " + dbt_interp_P
													+ " WHERE table_number=" + jl + " order by order_number");
				while (table_common.next()) {	
					table_common_values[vpointer] = table_common.getDouble("common_value");
					vpointer++;
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
	}


    /***************************************************************************
    *  The function interpolation_table_offset calculates
    *   the offset for the given table data
    *	in the all primary values array (the same for the common values array)
    *   The offset is returned
    *
    ***************************************************************************/
    static int interpolation_table_offset(int table_number)
	{
		int il;
		int vpointer = 0;

		if (n_interpolation_tables != 0)
			read_interpolation_tables();	// if the tables are not read in yet 

		// calculate the number of interpolation tables and the table sizes

		for (il = 0; il < (table_number-1); il++)
			vpointer += table_size[il];

		return vpointer;
	}

    /***************************************************************************
    *  reading all multifunction tables from the data Base table
    *       n_mfc_transform_tables contains the number of
    *                  all multifunction transform tables,
    *       before the first need of any multifunction transform table
    *       n_mfc_transform_tables = 0, after that the function downloads all
    *                  multifunction transform tables into the memory.
    *
    ***************************************************************************/
    static void read_multifunction_transforms()
	{
		 int n_table_lines;	// total number of lines
		 int il, jl;
		 int  vpointer;		// work variable for loops 
		 
		 if (n_mfc_transform_tables != 0)
			 return;
		 
		// calculate the number of multifunction tables and the table sizes
		// initialize the table sizes

		for (il = 0; il < N_MFCT_TABLES_MAX; il++)
			mfct_table_size[il] = 0;

		n_table_lines = 0;		// initial value
		try {
			final ResultSet mfct_table_elements = getDbServer("adbs").executeQuery("SELECT table_number FROM " + dbt_multi_P);
			
			while (mfct_table_elements.next() ) {	
				il= mfct_table_elements.getInt("table_number");
				mfct_table_size[il-1]++;
				if (mfct_table_size[il-1] == 1)
					n_mfc_transform_tables++;
				n_table_lines++;
			}
		}  catch (SQLException ex) {
			ex.printStackTrace();
		}
				 
		all_transform_values = new TransformTableData[N_MFCT_TABLES_MAX*MAX_MFCT_TABLE_SIZE];
		 
		// read the multifunction transform values
		vpointer = 0;
		for (il = 0; il < n_mfc_transform_tables; il++ ) {	
			jl = il+1;		// table number
			try {
				final ResultSet table_transform = getDbServer("adbs").executeQuery("SELECT table_number,order_number,common_transform, " +
														"primary_initial,primary_final,common_initial,common_final," +
														"const1,const2,const3,const4,const5," +
														"const6,const7,const8,const9,const10 FROM "+dbt_multi_P+
														" WHERE table_number=" + jl + " order by order_number");
				while (table_transform.next() ) {	
					all_transform_values[vpointer] = new TransformTableData();
					all_transform_values[vpointer].table_number =
					   table_transform.getInt("table_number");
					all_transform_values[vpointer].order_number =
					   table_transform.getInt("order_number");
					all_transform_values[vpointer].common_transform =
					   table_transform.getInt("common_transform");

					all_transform_values[vpointer].primary_initial =
					table_transform.getDouble("primary_initial");
					all_transform_values[vpointer].primary_final =
					table_transform.getDouble("primary_final");
					all_transform_values[vpointer].common_initial =
					table_transform.getDouble("common_initial");
					all_transform_values[vpointer].common_final =
					table_transform.getDouble("common_final");

					all_transform_values[vpointer].cx[0] =
					table_transform.getDouble("const1");
					all_transform_values[vpointer].cx[1] =
					table_transform.getDouble("const2");
					all_transform_values[vpointer].cx[2] =
					table_transform.getDouble("const3");
					all_transform_values[vpointer].cx[3] =
					table_transform.getDouble("const4");
					all_transform_values[vpointer].cx[4] =
					table_transform.getDouble("const5");
					all_transform_values[vpointer].cx[5] =
					table_transform.getDouble("const6");
					all_transform_values[vpointer].cx[6] =
					table_transform.getDouble("const7");
					all_transform_values[vpointer].cx[7] =
					table_transform.getDouble("const8");
					all_transform_values[vpointer].cx[8] =
					table_transform.getDouble("const9");
					all_transform_values[vpointer].cx[9] =
					table_transform.getDouble("const10");
					vpointer++;
				}
			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
  	 }

    /***************************************************************************
    *  The function mfct_table_offset calculates
    *   the offset for the given multifunction common transform table
    *	in the all transform values array.
    *   The offset is returned
    *
    ******************************************************************************/
    static int mfct_table_offset(int table_number)
	{
		int il;
		int vpointer = 0;

		if (n_mfc_transform_tables != 0)
			read_multifunction_transforms();	// if the tables are not read in yet 

		// calculate the number of interpolation tables and the table sizes
		for (il = 0; il < (table_number-1); il++)
			vpointer += mfct_table_size[il];

		return vpointer;
	}
}
